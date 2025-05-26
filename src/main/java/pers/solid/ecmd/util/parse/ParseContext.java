package pers.solid.ecmd.util.parse;

import com.mojang.brigadier.StringReader;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.ecmd.argument.SuggestedParser;

/**
 * @param registryAccess  此对象常用于命令中，用于从注册表中获取一些信息，常见于方块、物品、实体等的 ID 解析过程中。
 * @param parser          包含 {@link StringReader} 和 {@code List<SuggestionProvider>} 的对象。解析过程中，可以移动其 {@code cursor}，并指定如何提供建议。
 * @param suggestionsOnly 解析过程中是否为提供建议。如果为 {@code true}，那么一些不影响后续解析过程的操作可以不进行。
 * @param allowSparse     对于特定类型的语法，是否允许各部分用空格隔开。一般来说，直接用作命令参数、外面没有括号时，是 {@code false}。如果是在括号（或有明显其他割开定界符的环境）内解析，则为 {@code true}。
 * @param <S>
 */
public record ParseContext<S>(CommandRegistryAccess registryAccess, SuggestedParser<S> parser, boolean suggestionsOnly, boolean allowSparse) {
  public ParseContext<S> withSuggestionsOnly(boolean suggestionsOnly) {
    if (this.suggestionsOnly == suggestionsOnly) {
      return this;
    }
    return new ParseContext<>(registryAccess, parser, suggestionsOnly, allowSparse);
  }

  public ParseContext<S> withAllowSparse(boolean allowSparse) {
    if (this.allowSparse == allowSparse) {
      return this;
    }
    return new ParseContext<>(registryAccess, parser, suggestionsOnly, allowSparse);
  }
}
