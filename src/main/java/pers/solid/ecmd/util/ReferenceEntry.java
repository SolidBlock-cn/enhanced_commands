package pers.solid.ecmd.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.function.FailableSupplier;
import org.jetbrains.annotations.Nullable;
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
  static <T extends ReferenceEntry<T, E>, E> MapCodec<T> createCodec(ResourceKey<Registry<E>> registryKey, Codec<E> codec, Function<Holder.Reference<E>, T> function) {
    return RecordCodecBuilder.mapCodec(i -> i.group(RegistryFileCodec.create(registryKey, codec).comapFlatMap(holder -> holder instanceof Holder.Reference<E> reference ? DataResult.success(reference) : DataResult.error(() -> "inline not supported"), Function.identity()).fieldOf("value").forGetter(ReferenceEntry::value)).apply(i, function));
  }

  ResourceKey<? extends Registry<E>> registryKey();

  Holder.Reference<E> value();

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
    public @Nullable T parse(ParseContext<?> parseContext) throws CommandSyntaxException {
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
          return DefaultNamespace.ENHANCED_COMMANDS.suggestIdentifiers(parseContext.registries().lookupOrThrow(registryKey).listElementIds().map(ResourceKey::location), builder.createOffset(cursorBeforeId));
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
        final HolderGetter.Provider registryLookup = parseContext.registries().asGetterLookup();
        final ResourceKey<E> entryKey = ResourceKey.create(registryKey, id);
        final Optional<HolderGetter<E>> registryEntryLookup = registryLookup.lookup(registryKey);
        if (registryEntryLookup.isEmpty()) {
          // 考虑到有时客户端在解析命令时，会不知道该数据包中的内容，不应在客户端判定为解析错误。
          return Holder.Reference.createStandAlone(null, entryKey); // todo null safe?
        }
        final Optional<Holder.Reference<E>> entry = registryEntryLookup.get().get(entryKey);
        if (entry.isEmpty()) {
          reader.setCursor(cursorBeforeId);
          throw EnhancedCommandSyntaxException.withCursorEnd(createExceptionForUnknownId(reader, id.toString()), cursorAfterId);
        }
        return entry.get();
      });
    }

    protected abstract T getResultByEntrySupplier(FailableSupplier<Holder.Reference<E>, CommandSyntaxException> supplier) throws CommandSyntaxException;

    protected abstract CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier);
  }
}
