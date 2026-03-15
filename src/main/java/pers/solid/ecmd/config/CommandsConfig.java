package pers.solid.ecmd.config;

import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.config.annotations.NoDescription;

@NoDescription
public class CommandsConfig implements Cloneable {
  public static final CommandsConfig DEFAULT = new CommandsConfig();
  public static CommandsConfig current = DEFAULT;

  public boolean enableDebugCommands = EnhancedCommands.isDevelopmentEnvironment();

  @NoDescription
  public boolean enableMoveCommand = true;

  @NoDescription
  public boolean enableStackCommand = true;

  @NoDescription
  public boolean enableMirrorCommand = true;

  @Override
  public CommandsConfig clone() {
    try {
      return (CommandsConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
