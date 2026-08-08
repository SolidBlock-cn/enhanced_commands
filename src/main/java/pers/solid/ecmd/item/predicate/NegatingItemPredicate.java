package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

public record NegatingItemPredicate(ItemPredicate predicate) implements PredicateBasedItemPredicate {
  public static final MapCodec<NegatingItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ItemPredicate.CODEC.fieldOf("predicate").forGetter(NegatingItemPredicate::predicate)).apply(i, NegatingItemPredicate::new));

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    return !predicate().test(stack, executionContext);
  }

  @Override
  public ItemPredicateType<NegatingItemPredicate> getType() {
    return ItemPredicateTypes.NOT;
  }

  @Override
  public String expressAsString() {
    return "!" + predicate().expressAsString();
  }
}
