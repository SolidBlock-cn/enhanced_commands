package pers.solid.ecmd.predicate.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ComponentPresenceItemPredicate<T>(DataComponentType<T> componentType) implements ItemPredicateEntry, ItemPredicateWithoutContext {
  private static final MapCodec<ComponentPresenceItemPredicate<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(DataComponentType.CODEC.fieldOf("component_type").forGetter(ComponentPresenceItemPredicate::componentType)).apply(i, ComponentPresenceItemPredicate::new));

  @Override
  public boolean test(ItemStack stack) {
    return stack.has(componentType);
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.COMPONENT_PRESENCE;
  }

  @Override
  public @NotNull String asString() {
    return "*[" + asEntryString() + "]";
  }

  @Override
  public String asEntryString() {
    return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType) + "";
  }

  public enum Type implements ItemPredicateType<ComponentPresenceItemPredicate<?>> {
    COMPONENT_PRESENCE_TYPE;

    @Override
    public @NotNull MapCodec<ComponentPresenceItemPredicate<?>> getCodec() {
      return CODEC;
    }
  }
}
