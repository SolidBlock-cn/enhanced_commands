package pers.solid.ecmd.item.predicate;

import com.google.common.base.Predicates;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public record UnknownItemPredicate(Predicate<ItemStack> forward) implements ItemPredicateWithoutContext {
  public static final MapCodec<UnknownItemPredicate> CODEC = MapCodec.unit(new UnknownItemPredicate(Predicates.alwaysFalse()));

  @Override
  public boolean test(ItemStack stack) {
    return forward.test(stack);
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.UNKNOWN;
  }

  @Override
  public @NotNull String asString() {
    return "<unknown>";
  }

  public enum Type implements ItemPredicateType<UnknownItemPredicate> {
    UNKNOWN_TYPE;

    @Override
    public @NotNull MapCodec<UnknownItemPredicate> getCodec() {
      return CODEC;
    }
  }
}
