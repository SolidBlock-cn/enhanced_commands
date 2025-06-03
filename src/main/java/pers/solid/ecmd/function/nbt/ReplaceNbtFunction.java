package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.block.ExecutionContext;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.predicate.nbt.NbtPredicateArgument;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

/**
 * 在 NBT 中进行替换，将符合谓词的值都应用指定的函数。
 */
public record ReplaceNbtFunction(@NotNull NbtPredicate predicate, @NotNull NbtFunction function) implements NbtFunction {
  public static final MapCodec<ReplaceNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtPredicate.CODEC.fieldOf("predicate").forGetter(ReplaceNbtFunction::predicate),
      NbtFunction.CODEC.fieldOf("function").forGetter(ReplaceNbtFunction::function)
  ).apply(i, ReplaceNbtFunction::new));

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return "replace(" + predicate.asString(false) + ", " + function.asString(false) + ")";
  }

  @Override
  public @NotNull NbtFunctionType<ReplaceNbtFunction> getType() {
    return Type.REPLACE_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
    return function.recursivelyApply(nbtElement, predicate, context);
  }

  public enum Type implements NbtFunctionType<ReplaceNbtFunction> {
    REPLACE_TYPE;

    @Override
    public MapCodec<ReplaceNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionParamsParser<NbtFunctionArgument> {
    private NbtPredicateArgument nbtPredicateArgument;
    private NbtFunctionArgument nbtFunctionArgument;

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> nbtPredicateArgument = NbtPredicateArgument.parse(parseContext, false, false);
        case 1 -> nbtFunctionArgument = NbtFunctionArgument.parse(parseContext, false, false);
      }
    }

    @Override
    public NbtFunctionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return source -> new ReplaceNbtFunction(nbtPredicateArgument.toAbsolute(source), nbtFunctionArgument.toAbsolute(source));
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }
  }
}
