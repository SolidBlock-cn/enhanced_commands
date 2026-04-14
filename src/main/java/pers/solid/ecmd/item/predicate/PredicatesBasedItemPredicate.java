package pers.solid.ecmd.item.predicate;

import java.util.List;

public interface PredicatesBasedItemPredicate extends ItemPredicate {
  List<ItemPredicate> predicates();
}
