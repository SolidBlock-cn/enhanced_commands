package pers.solid.ecmd.configs;

public class GeneralParsingConfig {
  public static final GeneralParsingConfig DEFAULT = new GeneralParsingConfig();
  public static GeneralParsingConfig CURRENT = DEFAULT;

  public boolean suggestionEmitDefaultNamespace = true;
  public boolean suggestNonDefaultNamespacedIds = true;
}
