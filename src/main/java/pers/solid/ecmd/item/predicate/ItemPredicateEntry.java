package pers.solid.ecmd.item.predicate;

public interface ItemPredicateEntry extends ItemPredicate {
  default String asEntryString() {
    return asString();
  }
}
