package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.ExecutionContext;

/**
 * 在 NBT 中进行替换，将符合谓词的值都应用指定的函数。
 */
public record ReplaceNbtFunction(@NotNull NbtPredicate predicate, @NotNull NbtFunction function) implements NbtFunction {
  public static final MapCodec<ReplaceNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtPredicate.CODEC.fieldOf("predicate").forGetter(ReplaceNbtFunction::predicate),
      NbtFunction.CODEC.fieldOf("function").forGetter(ReplaceNbtFunction::function)
  ).apply(i, ReplaceNbtFunction::new));

  @Override
  public @NotNull String asString() {
    return "replace(" + predicate.asString(false) + ", " + function.asString() + ")";
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

  public static class Parser implements FunctionLikeParser.SequentialParams<ReplaceNbtFunction> {
    private NbtPredicate nbtPredicate;
    private NbtFunction nbtFunction;

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> nbtPredicate = NbtPredicate.parse(parseContext, false, false);
        case 1 -> nbtFunction = NbtFunction.parse(parseContext, false, false);
      }
    }

    @Override
    public ReplaceNbtFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new ReplaceNbtFunction(nbtPredicate, nbtFunction);
    }

    @Override
    public int minSequentialParamsCount() {
      return 2;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }
  }
}
