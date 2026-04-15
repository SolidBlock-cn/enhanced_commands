package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public record ComponentPresenceItemPredicate<T>(DataComponentType<T> componentType) implements ItemPredicateEntry, ItemPredicateWithoutContext {
  public static final MapCodec<ComponentPresenceItemPredicate<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(DataComponentType.CODEC.fieldOf("component_type").forGetter(ComponentPresenceItemPredicate::componentType)).apply(i, ComponentPresenceItemPredicate::new));

  @Override
  public boolean test(ItemStack stack) {
    return stack.has(componentType);
  }

  @Override
  public ItemPredicateType<ComponentPresenceItemPredicate<?>> getType() {
    return ItemPredicateTypes.COMPONENT_PRESENCE;
  }

  @Override
  public String asString() {
    return "*[" + asEntryString() + "]";
  }

  @Override
  public String asEntryString() {
    return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType) + "";
  }
}
