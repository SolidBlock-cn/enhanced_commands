package pers.solid.ecmd.config;

import pers.solid.ecmd.EnhancedCommands;

public class CommandsConfig implements Cloneable {
  public static final CommandsConfig DEFAULT = new CommandsConfig();
  public static CommandsConfig current = DEFAULT;

  public boolean enableDebugCommands = EnhancedCommands.isDevelopmentEnvironment();

  @Override
  public CommandsConfig clone() {
    try {
      return (CommandsConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
