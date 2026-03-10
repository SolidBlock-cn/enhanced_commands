package pers.solid.ecmd.predicate.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record ComponentValueCheckItemPredicate<T>(DataComponentType<T> componentType, T value) implements ItemPredicateEntry {
  public static final MapCodec<ComponentValueCheckItemPredicate<?>> CODEC = DataComponentType.PERSISTENT_CODEC.dispatchMap("component_type", ComponentValueCheckItemPredicate::componentType, ComponentValueCheckItemPredicate::codecForComponentType);

  @Override
  public boolean test(ItemStack stack) {
    return Objects.equals(stack.get(componentType), value);
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.COMPONENT_VALUE_CHECK;
  }

  @Override
  public @NotNull String asString() {
    return "[" + asEntryString() + "]";
  }

  @Override
  public String asEntryString() {
    return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType) + "=" + componentType.codecOrThrow().encodeStart(NbtOps.INSTANCE, value).result().orElseThrow();
  }

  private static <T> MapCodec<ComponentValueCheckItemPredicate<T>> codecForComponentType(DataComponentType<T> type) {
    return type.codecOrThrow().fieldOf("value").xmap(t -> new ComponentValueCheckItemPredicate<>(type, t), ComponentValueCheckItemPredicate::value);
  }

  public enum Type implements ItemPredicateType<ComponentValueCheckItemPredicate<?>> {
    COMPONENT_VALUE_CHECK_TYPE;

    @Override
    public @NotNull MapCodec<ComponentValueCheckItemPredicate<?>> getCodec() {
      return CODEC;
    }
  }
}
