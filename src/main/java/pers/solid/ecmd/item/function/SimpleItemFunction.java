package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

public record SimpleItemFunction(Holder<Item> item) implements ItemFunction {
  public static final MapCodec<SimpleItemFunction> CODEC = BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").xmap(SimpleItemFunction::new, SimpleItemFunction::item);

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    return new ItemStack(item);
  }

  @Override
  public ItemFunctionType<SimpleItemFunction> getType() {
    return ItemFunctionTypes.SIMPLE;
  }

  @Override
  public String expressAsString() {
    return item.getRegisteredName();
  }
}
