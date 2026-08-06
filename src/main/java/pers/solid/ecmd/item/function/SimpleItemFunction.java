package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;

public record SimpleItemFunction(Holder<Item> item) implements ItemFunction {
  public static final MapCodec<SimpleItemFunction> CODEC = BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").xmap(SimpleItemFunction::new, SimpleItemFunction::item);

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
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

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(item);
  }
}
