package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

public record SetComponentItemFunction<T>(DataComponentType<T> component, T value) implements ItemFunctionEntry {
  public static final MapCodec<SetComponentItemFunction<?>> CODEC = DataComponentType.CODEC.dispatchMap("component", SetComponentItemFunction::component, SetComponentItemFunction::codecForDataComponentType);

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    itemStack.set(component, value);
    return itemStack;
  }

  @Override
  public ItemFunctionType<SetComponentItemFunction<?>> getType() {
    return ItemFunctionTypes.SET_COMPONENT;
  }

  @Override
  public String expressAsString() {
    return "set_component(" + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component) + "," + component.codecOrThrow().encodeStart(NbtOps.INSTANCE, value).getOrThrow() + ")";
  }

  @Override
  public String asEntryString() {
    return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component) + "=" + component.codecOrThrow().encodeStart(NbtOps.INSTANCE, value).getOrThrow();
  }

  private static <T> MapCodec<SetComponentItemFunction<T>> codecForDataComponentType(DataComponentType<T> dataComponentType) {
    return dataComponentType.codecOrThrow().fieldOf("value").xmap(t -> new SetComponentItemFunction<>(dataComponentType, t), SetComponentItemFunction::value);
  }
}
