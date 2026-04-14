package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

public enum EmptyItemFunction implements ItemFunction, ItemFunctionType<EmptyItemFunction> {
  INSTANCE;
  public static final MapCodec<EmptyItemFunction> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    return itemStack;
  }

  @Override
  public EmptyItemFunction getType() {
    return ItemFunctionTypes.EMPTY;
  }

  @Override
  public MapCodec<EmptyItemFunction> getCodec() {
    return CODEC;
  }

  @Override
  public String asString() {
    return "~";
  }
}
