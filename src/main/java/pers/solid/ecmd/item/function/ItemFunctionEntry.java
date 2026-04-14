package pers.solid.ecmd.item.function;

public interface ItemFunctionEntry extends ItemFunction {
  default String asEntryString() {
    return asString();
  }
}
