package pers.solid.ecmd.configs;

public class RegistryParsingConfig {
  public static final RegistryParsingConfig DEFAULT = new RegistryParsingConfig();
  public static RegistryParsingConfig CURRENT = DEFAULT;

  public boolean detailedUnknownRegistryEntry = true;
}
