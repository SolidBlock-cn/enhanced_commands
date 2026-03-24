package pers.solid.ecmd.predicate.item;

public interface ItemPredicateEntry extends ItemPredicate {
  default String asEntryString() {
    return asString();
  }
}
