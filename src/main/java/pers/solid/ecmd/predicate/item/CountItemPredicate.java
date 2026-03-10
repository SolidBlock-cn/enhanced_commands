package pers.solid.ecmd.predicate.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.accessor.ItemPredicateArgumentAccessor;
import pers.solid.ecmd.util.StringUtil;

public record CountItemPredicate(MinMaxBounds.Ints count) implements ItemPredicateEntry {
  public static final MapCodec<CountItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(CountItemPredicate::count)).apply(i, CountItemPredicate::new));

  @Override
  public boolean test(ItemStack stack) {
    return count.matches(stack.getCount());
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.COUNT;
  }

  @Override
  public @NotNull String asString() {
    return "[" + asEntryString() + "]";
  }

  @Override
  public String asEntryString() {
    return ItemPredicateArgumentAccessor.getCOUNT_ID() + "=" + StringUtil.wrapRange(count);
  }

  public enum Type implements ItemPredicateType<CountItemPredicate> {
    COUNT_TYPE;

    @Override
    public @NotNull MapCodec<CountItemPredicate> getCodec() {
      return CODEC;
    }
  }
}
