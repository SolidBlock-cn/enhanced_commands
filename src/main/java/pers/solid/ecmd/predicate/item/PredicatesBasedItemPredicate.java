package pers.solid.ecmd.predicate.item;

import java.util.List;

public interface PredicatesBasedItemPredicate extends ItemPredicate {
  List<ItemPredicate> predicates();
}
