package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public enum EmptyItemFunction implements ItemFunction, DoesNotRequireValidation {
  INSTANCE;
  public static final MapCodec<EmptyItemFunction> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    return itemStack;
  }

  @Override
  public ItemFunctionType<EmptyItemFunction> getType() {
    return ItemFunctionTypes.EMPTY;
  }

  @Override
  public String expressAsString() {
    return "~";
  }
}
