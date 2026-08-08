package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.function.WeightedListParser;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.Objects;

public record PickItemFunction(WeightedList<ItemFunction> functions) implements ItemFunction, RequiresValidation {
  public static final MapCodec<PickItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      WeightedList.createMapCodec(ItemFunction.CODEC).fieldOf("functions").forGetter(PickItemFunction::functions)
  ).apply(i, PickItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    return functions.getRandom(context.random).getModifiedStack(itemStack, originalStack, context);
  }

  @Override
  public ItemFunctionType<PickItemFunction> getType() {
    return ItemFunctionTypes.PICK;
  }

  @Override
  public String expressAsString() {
    return "pick(" + functions.asString(ExpressionConvertible::expressAsString) + ")";
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return functions;
  }

  public static class Parser implements FunctionContentParser<ItemFunction> {
    private @Nullable WeightedList<ItemFunction> weightedList;

    @Override
    public ItemFunction getParseResult(ParseContext<?> parseContext) {
      return new PickItemFunction(Objects.requireNonNull(weightedList, "weightedList"));
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final WeightedListParser<ItemFunction> weightedListParser = WeightedListParser.of((parseContext1) -> ItemFunction.parse(parseContext));
      weightedList = weightedListParser.parse(parseContext);
    }
  }
}
