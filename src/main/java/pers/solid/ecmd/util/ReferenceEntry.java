package pers.solid.ecmd.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.function.FailableSupplier;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Optional;
import java.util.function.Function;

/**
 * 所有通过数据包访问数据驱动对象的共用接口。
 *
 * @param <T> 该类自身所代表的类型。
 * @param <E> 该类的 entry 的类型。
 */
public interface ReferenceEntry<T extends ReferenceEntry<T, E>, E> {
  static <T extends ReferenceEntry<T, E>, E> MapCodec<T> createCodec(ResourceKey<Registry<E>> registryKey, Function<ResourceKey<E>, T> function) {
    return RecordCodecBuilder.mapCodec(i -> i.group(ResourceKey.codec(registryKey).fieldOf("id").forGetter(ReferenceEntry::id)).apply(i, function));
  }

  ResourceKey<E> id();

  default Holder.Reference<E> getEntry(HolderLookup.Provider registryLookup) throws CommandSyntaxException {
    final ResourceKey<E> id = id();
    return registryLookup.lookupOrThrow(id.registryKey()).get(id).orElseThrow(() -> createExceptionForUnknownId(null, id.location().toString()));
  }

  default E value(HolderLookup.Provider registryLookup) throws CommandSyntaxException {
    return getEntry(registryLookup).value();
  }

  CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier);

  abstract class PrefixedIdParser<T, E> implements Parser<T> {
    private final char prefix;
    private final Component tooltip;
    private final ResourceKey<Registry<E>> registryKey;

    protected PrefixedIdParser(char prefix, Component tooltip, ResourceKey<Registry<E>> registryKey) {
      this.prefix = prefix;
      this.tooltip = tooltip;
      this.registryKey = registryKey;
    }

    @Override
    public T parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString(Character.toString(prefix), tooltip, suggestionsBuilder).buildFuture());
      boolean suffixed = false;
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == prefix) {
        reader.skip();
        suffixed = true;
      }
      if (!suffixed) return null;
      parseContext.clearSuggestion();

      // try to optimize with id?
      final int cursorBeforeId = reader.getCursor();
      parseContext.setSuggestion((context, builder) -> {
        if (context.getSource() instanceof CommandSourceStack) {
          return DefaultNamespace.ENHANCED_COMMANDS.suggestIdentifiers(parseContext.registryAccess().lookupOrThrow(registryKey).listElementIds().map(ResourceKey::location), builder.createOffset(cursorBeforeId));
        } else if (context.getSource() instanceof SharedSuggestionProvider commandSource) {
          return commandSource.customSuggestion(context);
        } else {
          return Suggestions.empty();
        }
      });
      parseContext.terminateSuggestionsIfNotEmpty();
      if (parseContext.allowSparse()) reader.skipWhitespace();
      final ResourceLocation id = DefaultNamespace.ENHANCED_COMMANDS.fromStringReader(reader);
      final int cursorAfterId = reader.getCursor();
      return getResultByEntrySupplier(() -> {
        final HolderGetter.Provider registryLookup = parseContext.registryAccess().asGetterLookup();
        final ResourceKey<E> entryKey = ResourceKey.create(registryKey, id);
        final Optional<HolderGetter<E>> registryEntryLookup = registryLookup.lookup(registryKey);
        if (registryEntryLookup.isEmpty()) {
          // 考虑到有时客户端在解析命令时，会不知道该数据包中的内容，不应在客户端判定为解析错误。
          return entryKey;
        }
        final Optional<Holder.Reference<E>> entry = registryEntryLookup.get().get(entryKey);
        if (entry.isEmpty()) {
          reader.setCursor(cursorBeforeId);
          throw CommandSyntaxExceptionExtension.withCursorEnd(createExceptionForUnknownId(reader, id.toString()), cursorAfterId);
        }
        return entry.get().key();
      });
    }

    protected abstract T getResultByEntrySupplier(FailableSupplier<ResourceKey<E>, CommandSyntaxException> supplier) throws CommandSyntaxException;

    protected abstract CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier);
  }
}
