package pers.solid.ecmd.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 此类所有通过数据包访问可数据驱动对象的共用接口，例如方块谓词、方块函数等的“reference”类型，其通常是存储一个 Holder.Reference。
 *
 * @param <T> 该 reference 类自身所代表的类型
 * @param <E> 可数据驱动对象的类型
 */
public interface ReferenceEntry<T extends ReferenceEntry<T, E>, E> {
  static <T extends ReferenceEntry<T, E>, E> MapCodec<T> createCodec(ResourceKey<Registry<E>> registryKey, Codec<E> codec, Function<Holder.Reference<E>, T> function) {
    return RecordCodecBuilder.mapCodec(i -> i.group(RegistryFileCodec.create(registryKey, codec).comapFlatMap(holder -> holder instanceof Holder.Reference<E> reference ? DataResult.success(reference) : DataResult.error(() -> "inline not supported"), Function.identity()).fieldOf("value").forGetter(ReferenceEntry::value)).apply(i, function));
  }

  ResourceKey<? extends Registry<E>> registryKey();

  Holder.Reference<E> value();

  /**
   * 用于解析“$ + ID”形式的语法，并将其转化为 ReferenceEntry 的对象，在解析过程中，能自动提供关于 ID 的建议，并为不存在的 ID 报错。
   *
   * @param <T> 该 reference 类自身所代表的类型
   * @param <E> 可数据驱动对象的类型
   */
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
      boolean prefixed = false;
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == prefix) {
        reader.skip();
        prefixed = true;
      }
      if (!prefixed) return null;
      parseContext.clearSuggestion();

      final Holder.Reference<E> holderReference = parseAndGetHolderReference(parseContext);
      return getResultByHolderReference(holderReference);
    }

    /**
     * 解析前缀之后的 ID 的内容，并返回一个 Holder.Reference。
     */
    public Holder.Reference<E> parseAndGetHolderReference(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final int cursorBeforeId = reader.getCursor();
      parseContext.setSuggestion((context, builder) -> getIdSuggestion(parseContext, context, builder, cursorBeforeId));
      parseContext.terminateSuggestionsIfNotEmpty();
      if (parseContext.allowSparse()) reader.skipWhitespace();
      final ResourceLocation id = DefaultNamespace.ENHANCED_COMMANDS.fromStringReader(reader);
      final int cursorAfterId = reader.getCursor();
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
    }

    private CompletableFuture<Suggestions> getIdSuggestion(ParseContext<?> parseContext, @UnknownNullability CommandContext<?> context, @UnknownNullability SuggestionsBuilder builder, int cursorBeforeId) {
      if (context.getSource() instanceof CommandSourceStack) {
        return DefaultNamespace.ENHANCED_COMMANDS.suggestIdentifiers(parseContext.registries().lookupOrThrow(registryKey).listElementIds().map(ResourceKey::location), builder.createOffset(cursorBeforeId));
      } else if (context.getSource() instanceof SharedSuggestionProvider commandSource) {
        return commandSource.customSuggestion(context);
      } else {
        return Suggestions.empty();
      }
    }

    protected abstract T getResultByHolderReference(Holder.Reference<E> holderReference) throws CommandSyntaxException;

    protected abstract CommandSyntaxException createExceptionForUnknownId(StringReader reader, String identifier);
  }

  /**
   * 用于解析“reference(id)”形式的函数式语法，在解析时，需要提供一个 {@link PrefixedIdParser} 以辅助 ID 的建议和解析。
   *
   * @param <T> 该 reference 类自身所代表的类型
   * @param <E> 可数据驱动对象的类型
   */
  class ReferenceFunctionGrammarParser<T, E> implements FunctionContentParser<T> {
    /**
     * 与函数语法解析器相关的 {@link PrefixedIdParser}，用于提供 ID 建议和解析。
     */
    public final PrefixedIdParser<T, E> affiliatedPrefixedIdParser;


    private @Nullable Holder.Reference<E> holderReference = null;

    public ReferenceFunctionGrammarParser(PrefixedIdParser<T, E> affiliatedPrefixedIdParser) {
      this.affiliatedPrefixedIdParser = affiliatedPrefixedIdParser;
    }

    @Override
    public T getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      Objects.requireNonNull(holderReference, "value");
      return affiliatedPrefixedIdParser.getResultByHolderReference(holderReference);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      holderReference = affiliatedPrefixedIdParser.parseAndGetHolderReference(parseContext);
    }
  }
}
