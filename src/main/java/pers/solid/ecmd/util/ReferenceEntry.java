package pers.solid.ecmd.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.function.FailableSupplier;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Optional;
import java.util.function.Function;

/**
 * 所有通过数据包访问数据驱动对象的共用接口。
 *
 * @param <T> 该类自身所代表的类型。
 * @param <E> 该类的 entry 的类型。
 */
public interface ReferenceEntry<T extends ReferenceEntry<T, E>, E> {
  static <T extends ReferenceEntry<T, E>, E> MapCodec<T> createCodec(RegistryKey<Registry<E>> registryKey, Function<RegistryKey<E>, T> function) {
    return RecordCodecBuilder.mapCodec(i -> i.group(RegistryKey.createCodec(registryKey).fieldOf("id").forGetter(ReferenceEntry::id)).apply(i, function));
  }

  RegistryKey<E> id();

  default RegistryEntry.Reference<E> getEntry(RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    final RegistryKey<E> id = id();
    return registryLookup.getWrapperOrThrow(id.getRegistryRef()).getOptional(id).orElseThrow(() -> createExceptionForUnknownId(null, id.getValue().toString()));
  }

  default E value(RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    return getEntry(registryLookup).value();
  }

  CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier);

  abstract class PrefixedIdParser<T, E> implements Parser<T> {
    private final char prefix;
    private final Text tooltip;
    private final RegistryKey<Registry<E>> registryKey;

    protected PrefixedIdParser(char prefix, Text tooltip, RegistryKey<Registry<E>> registryKey) {
      this.prefix = prefix;
      this.tooltip = tooltip;
      this.registryKey = registryKey;
    }

    @Override
    public T parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {

      parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString(Character.toString(prefix), tooltip, suggestionsBuilder).buildFuture());
      boolean suffixed = false;
      if (parser.reader.canRead() && parser.reader.peek() == prefix) {
        parser.reader.skip();
        suffixed = true;
      }
      if (!suffixed) return null;
      parser.clearSuggestion();

      // try to optimize with id?
      final int cursorBeforeId = parser.reader.getCursor();
      parser.setSuggestion((context, builder) -> {
        if (context.getSource() instanceof ServerCommandSource) {
          return DefaultNamespace.ENHANCED_COMMANDS.suggestIdentifiers(registryAccess.getWrapperOrThrow(registryKey).streamKeys().map(RegistryKey::getValue), builder.createOffset(cursorBeforeId));
        } else if (context.getSource() instanceof CommandSource commandSource) {
          return commandSource.getCompletions(context);
        } else {
          return Suggestions.empty();
        }
      });
      parser.terminateSuggestionsIfNotEmpty();
      if (allowSparse) parser.reader.skipWhitespace();
      final Identifier id = DefaultNamespace.ENHANCED_COMMANDS.fromStringReader(parser.reader);
      final int cursorAfterId = parser.reader.getCursor();
      return getResultByEntrySupplier(() -> {
        final Optional<RegistryEntry.Reference<E>> entry = registryAccess.createRegistryLookup().getOptionalEntry(registryKey, RegistryKey.of(registryKey, id));
        if (entry.isEmpty()) {
          parser.reader.setCursor(cursorBeforeId);
          throw CommandSyntaxExceptionExtension.withCursorEnd(createExceptionForUnknownId(parser.reader, id.toString()), cursorAfterId);
        }
        return entry.get().registryKey();
      });
    }

    protected abstract T getResultByEntrySupplier(FailableSupplier<RegistryKey<E>, CommandSyntaxException> supplier);

    protected abstract CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier);
  }
}
