package pers.solid.ecmd.predicate.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.List;
import java.util.stream.Collectors;

public record AllItemPredicate(List<ItemPredicate> predicates) implements PredicatesBasedItemPredicate {
  public static final MapCodec<AllItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ItemPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AllItemPredicate::predicates)).apply(i, AllItemPredicate::new));

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    return predicates.stream().allMatch(p -> p.test(stack, executionContext));
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.ALL_TYPE;
  }

  @Override
  public @NotNull String asString() {
    return "all(" + predicates.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + ")";
  }

  public enum Type implements ItemPredicateType<AllItemPredicate> {
    ALL_TYPE;

    @Override
    public @NotNull MapCodec<AllItemPredicate> getCodec() {
      return CODEC;
    }
  }
}
