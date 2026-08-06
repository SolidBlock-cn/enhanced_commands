package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public record RemoveComponentItemFunction<T>(DataComponentType<T> component) implements ItemFunctionEntry, DoesNotRequireValidation {
  public static final MapCodec<RemoveComponentItemFunction<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      DataComponentType.CODEC.fieldOf("component").forGetter(RemoveComponentItemFunction::component)
  ).apply(i, RemoveComponentItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    itemStack.remove(component);
    return itemStack;
  }

  @Override
  public ItemFunctionType<RemoveComponentItemFunction<?>> getType() {
    return ItemFunctionTypes.REMOVE_COMPONENT;
  }

  @Override
  public String expressAsString() {
    return "remove_component(" + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component) + ")";
  }

  @Override
  public String asEntryString() {
    return "!" + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component);
  }
}
