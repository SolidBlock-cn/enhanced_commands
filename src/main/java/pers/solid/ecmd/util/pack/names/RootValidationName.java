package pers.solid.ecmd.util.pack.names;

public enum RootValidationName implements ValidationName {
  INSTANCE;

  @Override
  public String asString() {
    return "{root}";
  }
}
