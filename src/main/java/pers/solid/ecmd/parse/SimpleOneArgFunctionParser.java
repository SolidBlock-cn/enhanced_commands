package pers.solid.ecmd.parse;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SimpleOneArgFunctionParser<T, R> implements FunctionContentParser.SequentialParams<R> {
  public static final Component NOT_PREDICATE_DESCRIPTION = Component.translatable("enhanced_commands.predicate.not");

  private @Nullable T element;

  public final FailableFunction<ParseContext<?>, T, CommandSyntaxException> elementParser;
  public final FailableFunction<T, R, CommandSyntaxException> resultProvider;

  public SimpleOneArgFunctionParser(FailableFunction<ParseContext<?>, T, CommandSyntaxException> elementParser, FailableFunction<T, R, CommandSyntaxException> resultProvider) {
    this.elementParser = elementParser;
    this.resultProvider = resultProvider;
  }

  @Override
  public @Nullable R getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
    Objects.requireNonNull(element, "element");
    return resultProvider.apply(element);
  }

  @Override
  public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
    element = elementParser.apply(parseContext);
  }

  @Override
  public int minSequentialParamsCount() {
    return 1;
  }

  @Override
  public int maxSequentialParamsCount() {
    return 1;
  }
}
