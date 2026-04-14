package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.List;
import java.util.stream.Collectors;

public record OverlayItemFunction(List<ItemFunction> functions) implements ItemFunction {
  public static final MapCodec<OverlayItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      ItemFunction.CODEC.listOf().fieldOf("functions").forGetter(OverlayItemFunction::functions)
  ).apply(i, OverlayItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    for (ItemFunction function : functions) {
      itemStack = function.getModifiedStack(itemStack, originalStack, context);
    }
    return itemStack;
  }

  @Override
  public ItemFunctionType<OverlayItemFunction> getType() {
    return ItemFunctionTypes.OVERLAY;
  }

  @Override
  public String asString() {
    return functions.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ", "overlay(", ")"));
  }

  public enum Type implements ItemFunctionType<OverlayItemFunction> {
    OVERLAY_TYPE;

    @Override
    public MapCodec<OverlayItemFunction> getCodec() {
      return CODEC;
    }
  }
}
