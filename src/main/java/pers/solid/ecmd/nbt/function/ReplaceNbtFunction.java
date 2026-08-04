package pers.solid.ecmd.nbt.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.List;
import java.util.Objects;

/**
 * 在 NBT 中进行替换，将符合谓词的值都应用指定的函数。
 */
public record ReplaceNbtFunction(NbtPredicate predicate, NbtFunction function) implements NbtFunction, RequiresValidation {
  public static final MapCodec<ReplaceNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtPredicate.CODEC.fieldOf("predicate").forGetter(ReplaceNbtFunction::predicate),
      NbtFunction.CODEC.fieldOf("function").forGetter(ReplaceNbtFunction::function)
  ).apply(i, ReplaceNbtFunction::new));

  @Override
  public String expressAsString() {
    return "replace(" + predicate.asString(false) + ", " + function.expressAsString() + ")";
  }

  @Override
  public NbtFunctionType<ReplaceNbtFunction> getType() {
    return NbtFunctionTypes.REPLACE;
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    return function.recursivelyApply(nbtElement, predicate, context);
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(predicate, function);
  }

  public static class Parser implements FunctionContentParser.SequentialParams<ReplaceNbtFunction> {
    private @Nullable NbtPredicate nbtPredicate;
    private @Nullable NbtFunction nbtFunction;

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> nbtPredicate = NbtPredicate.parse(parseContext, false, false);
        case 1 -> nbtFunction = NbtFunction.parse(parseContext, false, false);
      }
    }

    @Override
    public ReplaceNbtFunction getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(nbtPredicate, "nbtPredicate");
      Objects.requireNonNull(nbtFunction, "nbtFunction");
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
