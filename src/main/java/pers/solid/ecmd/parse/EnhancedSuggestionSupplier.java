package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.ResourceSuggestion;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * <p>在 packrat 中提供自定义建议的 {@link SuggestionsBuilder}。
 * <p>原版的 {@link SuggestionsBuilder} 只能提供字符串建议，{@link ResourceSuggestion} 亦只能提供简单的 ID 的建议，需要使用非 packrat 的方法提供更加复杂的建议，包括让客户端从服务器请求建议，则需要更加复杂的处理。本模组通过 mixin，使得实现了 {@link EnhancedSuggestionSupplier} 的建议项会在获取建议时调用 {@link #forceGetSuggestionsUnchecked} 来获取建议。
 * <p>请注意，原版的 {@link #possibleValues} 或 {@link ResourceSuggestion#possibleValues}（如果实现了 {@link ResourceSuggestion}）如果返回了非空的建议，则这些建议也会正常生效。
 *
 * @see ParseContext#buildSuggestions(CommandContext, SuggestionsBuilder)
 * @see Grammar#parseForSuggestions(SuggestionsBuilder)
 */
@FunctionalInterface
public interface EnhancedSuggestionSupplier extends SuggestionSupplier<StringReader> {
  @Override
  default Stream<String> possibleValues(ParseState<StringReader> parseState) {
    return Stream.empty();
  }

  /**
   * 使用 {@link SuggestionsBuilder} 提供命令建议。
   */
  CompletableFuture<Suggestions> forceGetSuggestionsUnchecked(SuggestionsBuilder builder) throws CommandSyntaxException;

  /**
   * 创建一个简单地使用 {@link SuggestionProvider} 的建议，就像原版大多数命令参数的解析方法一样。
   *
   * @implNote 由于原版的解析 Packrat 时，没有 {@link CommandContext}，因此这里会使用 {@link MixinShared#commandContextForPackrat} 字段来存储。
   */
  @SuppressWarnings("unchecked")
  static <S> EnhancedSuggestionSupplier of(SuggestionProvider<S> suggestionProvider) {
    return builder -> suggestionProvider.getSuggestions((CommandContext<S>) MixinShared.commandContextForPackrat, builder);
  }
}
