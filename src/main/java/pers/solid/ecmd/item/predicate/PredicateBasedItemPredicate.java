package pers.solid.ecmd.item.predicate;

public interface PredicateBasedItemPredicate extends ItemPredicate {
  ItemPredicate predicate();
}
