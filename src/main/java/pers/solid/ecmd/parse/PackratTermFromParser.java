package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.parsing.packrat.*;
import org.jetbrains.annotations.Nullable;

public record PackratTermFromParser<T>(HolderLookup.Provider registries, Atom<T> atom, Parser<T> parser) implements Term<StringReader> {
  @Override
  public boolean parse(ParseState<StringReader> parseState, Scope scope, Control control) {
    final StringReader input = parseState.input();

    // 由于原版的 term 均会在解析的最开始处跳过空格，且不跳过空格会影响解析，因此这里也跳过空格。
    input.skipWhitespace();
    final int mark = parseState.mark();

    final ParseContext<Object> parseContext = new ParseContext<>(registries, input, false, true);
    final @Nullable T parse;
    try {
      parse = parser.parse(parseContext);
    } catch (CommandSyntaxException e) {
      parseState.errorCollector().store(input.getCursor(), new EnhancedSuggestionSupplier<>(parseContext::buildSuggestions), e);
      return false;
    }
    parseState.errorCollector().store(mark, new EnhancedSuggestionSupplier<>(parseContext::buildSuggestions), CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create());
    if (parse != null) {
      // 解析到了内容，并直接将其输出。
      scope.put(atom, parse);
      return true;
    } else {
      // 未解析到符合条件的内容，也没有抛出异常。
      return false;
    }
  }
}
