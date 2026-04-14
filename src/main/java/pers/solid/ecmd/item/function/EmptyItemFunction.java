package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;

public enum EmptyItemFunction implements ItemFunction, ItemFunctionType<EmptyItemFunction> {
  INSTANCE;
  public static final MapCodec<EmptyItemFunction> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public @NotNull ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    return itemStack;
  }

  @Override
  public @NotNull EmptyItemFunction getType() {
    return ItemFunctionTypes.EMPTY;
  }

  @Override
  public @NotNull MapCodec<EmptyItemFunction> getCodec() {
    return CODEC;
  }

  @Override
  public @NotNull String asString() {
    return "~";
  }
}
