package pers.solid.ecmd.parse;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SimpleListFunctionParser<T, R> implements FunctionContentParser.SequentialParams<R> {
  public static final Component ALL_PREDICATE_DESCRIPTION = Component.translatable("enhanced_commands.predicate.all");
  public static final Component ANY_PREDICATE_DESCRIPTION = Component.translatable("enhanced_commands.predicate.any");
  private final List<T> values = new ArrayList<>();

  public final FailableFunction<ParseContext<?>, T, CommandSyntaxException> elementParser;
  public final FailableFunction<@Unmodifiable List<T>, R, CommandSyntaxException> resultProvider;

  public SimpleListFunctionParser(FailableFunction<ParseContext<?>, T, CommandSyntaxException> elementParser, FailableFunction<@Unmodifiable List<T>, R, CommandSyntaxException> resultProvider) {
    this.elementParser = elementParser;
    this.resultProvider = resultProvider;
  }

  @Override
  public @Nullable R getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
    return resultProvider.apply(ImmutableList.copyOf(values));
  }

  @Override
  public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
    values.add(elementParser.apply(parseContext));
  }
}
