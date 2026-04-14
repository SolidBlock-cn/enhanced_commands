package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;

public record NegatingItemPredicate(ItemPredicate predicate) implements PredicateBasedItemPredicate {
  public static final MapCodec<NegatingItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ItemPredicate.CODEC.fieldOf("predicate").forGetter(NegatingItemPredicate::predicate)).apply(i, NegatingItemPredicate::new));

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    return !predicate().test(stack, executionContext);
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.NEGATING;
  }

  @Override
  public @NotNull String asString() {
    return "!" + predicate().asString();
  }

  public enum Type implements ItemPredicateType<NegatingItemPredicate> {
    NEGATING_TYPE;

    @Override
    public @NotNull MapCodec<NegatingItemPredicate> getCodec() {
      return CODEC;
    }
  }
}
