package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record SimpleCombinationItemPredicate(ItemPredicate itemType, List<ItemPredicate> components) implements ItemPredicateEntry {
  public static final MapCodec<SimpleCombinationItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      ItemPredicate.CODEC.comapFlatMap(predicate -> {
        try {
          checkValidityForItemType(predicate);
          return DataResult.success(predicate);
        } catch (IllegalArgumentException e) {
          return DataResult.error(e::getMessage);
        }
      }, Function.identity()).optionalFieldOf("item_type", ConstantItemPredicate.ALWAYS_TRUE).forGetter(SimpleCombinationItemPredicate::itemType),
      ItemPredicate.CODEC.comapFlatMap(predicate -> {
        try {
          checkValidityForComponents(predicate);
          return DataResult.success(predicate);
        } catch (IllegalArgumentException e) {
          return DataResult.error(e::getMessage);
        }
      }, Function.identity()).listOf().optionalFieldOf("components", List.of()).forGetter(SimpleCombinationItemPredicate::components)
  ).apply(i, SimpleCombinationItemPredicate::new));

  public SimpleCombinationItemPredicate(ItemPredicate itemType) {
    this(itemType, List.of());
  }

  public SimpleCombinationItemPredicate {
    checkValidityForItemType(itemType);

    for (ItemPredicate componentPredicate : components) {
      checkValidityForComponents(componentPredicate);
    }
  }

  private static boolean isValidTypeForItemType(ItemPredicate predicate) {
    return predicate instanceof SimpleItemPredicate || predicate instanceof TagItemPredicate || predicate == ConstantItemPredicate.ALWAYS_TRUE;
  }

  private static boolean isValidTypeForComponents(ItemPredicate predicate) {
    return true;
  }

  private static void checkValidityForItemType(ItemPredicate predicate) {
    if (!isValidTypeForItemType(predicate)) {
      throw new IllegalArgumentException("item_type for SimpleCombinationItemPredicate only supports types of simple_item, simple_tag and always-true constant.");
    }
  }

  private static void checkValidityForComponents(ItemPredicate predicate) {
    if (isValidTypeForComponents(predicate)) {
      return;
    }

    if (predicate instanceof NegatingItemPredicate predicateBased) {
      checkValidityForComponents(predicateBased.predicate());
      return;
    } else if (predicate instanceof AnyItemPredicate predicatesBased) {
      for (ItemPredicate itemPredicate : predicatesBased.predicates()) {
        checkValidityForComponents(itemPredicate);
      }
      return;
    }

    throw new IllegalArgumentException("components of SimpleCombinationItemPredicate does not support type of " + predicate.getClass().getSimpleName());
  }

  public static SimpleCombinationItemPredicate of(List<ItemPredicate> predicates) {
    if (predicates.isEmpty()) {
      return new SimpleCombinationItemPredicate(ConstantItemPredicate.ALWAYS_TRUE);
    } else {
      final ItemPredicate first = predicates.get(0);
      if (isValidTypeForItemType(first)) {
        return new SimpleCombinationItemPredicate(first, List.copyOf(predicates.subList(1, predicates.size())));
      } else {
        return new SimpleCombinationItemPredicate(ConstantItemPredicate.ALWAYS_TRUE, predicates);
      }
    }
  }

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    return itemType.test(stack, executionContext) && components.stream().allMatch(p -> p.test(stack, executionContext));
  }

  @Override
  public ItemPredicateType<SimpleCombinationItemPredicate> getType() {
    return ItemPredicateTypes.SIMPLE_COMBINATION;
  }

  @Override
  public String expressAsString() {
    return itemType.expressAsString() + (components.isEmpty() ? "" : components.stream().map(SimpleCombinationItemPredicate::toEntryString).collect(Collectors.joining(", ", "[", "]")));
  }

  @Override
  public String asEntryString() {
    return "(" + expressAsString() + ")";
  }

  private static String toEntryString(ItemPredicate predicate) {
    if (predicate instanceof NegatingItemPredicate negating) {
      return "!" + toEntryString(negating.predicate());
    } else if (predicate instanceof AnyItemPredicate any) {
      return any.predicates().stream().map(SimpleCombinationItemPredicate::toEntryString).collect(Collectors.joining(" | "));
    } else if (predicate instanceof ItemPredicateEntry entry) {
      return entry.asEntryString();
    } else {
      return "(" + predicate.expressAsString() + ")";
    }
  }
}
