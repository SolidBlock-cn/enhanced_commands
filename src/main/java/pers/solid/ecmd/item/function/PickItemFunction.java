package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

public record PickItemFunction(WeightedList<ItemFunction> functions) implements ItemFunction {
  public static final MapCodec<PickItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      WeightedList.createMapCodec(ItemFunction.CODEC).fieldOf("functions").forGetter(PickItemFunction::functions)
  ).apply(i, PickItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    return functions.getRandom(context.random).getModifiedStack(itemStack, originalStack, context);
  }

  @Override
  public ItemFunctionType<PickItemFunction> getType() {
    return ItemFunctionTypes.PICK;
  }

  @Override
  public String asString() {
    return functions.asString(ExpressionConvertible::asString);
  }

  public enum Type implements ItemFunctionType<PickItemFunction> {
    PICK_TYPE;

    @Override
    public MapCodec<PickItemFunction> getCodec() {
      return CODEC;
    }
  }
}
