package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.List;
import java.util.stream.Collectors;

public record AnyItemPredicate(List<ItemPredicate> predicates) implements PredicatesBasedItemPredicate {
  public static final MapCodec<AnyItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ItemPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AnyItemPredicate::predicates)).apply(i, AnyItemPredicate::new));

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    return predicates.stream().anyMatch(p -> p.test(stack, executionContext));
  }

  @Override
  public ItemPredicateType<AnyItemPredicate> getType() {
    return ItemPredicateTypes.ANY_TYPE;
  }

  @Override
  public String expressAsString() {
    return "any(" + predicates.stream().map(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ")) + ")";
  }
}
