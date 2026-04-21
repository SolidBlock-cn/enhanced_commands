package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record SimpleItemPredicate(Item item) implements ItemPredicateWithoutContext {
  public static final Codec<SimpleItemPredicate> STRING_BASED_CODEC = BuiltInRegistries.ITEM.byNameCodec().xmap(SimpleItemPredicate::new, SimpleItemPredicate::item);

  public static final MapCodec<SimpleItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(SimpleItemPredicate::item)).apply(i, SimpleItemPredicate::new));

  @Override
  public boolean test(ItemStack stack) {
    return stack.is(item);
  }

  @Override
  public ItemPredicateType<SimpleItemPredicate> getType() {
    return ItemPredicateTypes.SIMPLE;
  }

  @Override
  public String expressAsString() {
    return BuiltInRegistries.ITEM.getKey(item).toString();
  }
}
