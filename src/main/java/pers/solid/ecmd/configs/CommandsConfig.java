package pers.solid.ecmd.configs;

import net.fabricmc.loader.api.FabricLoader;

public class CommandsConfig implements Cloneable {
  public static final CommandsConfig DEFAULT = new CommandsConfig();
  public static CommandsConfig CURRENT = DEFAULT;

  public boolean enableDebugCommands = FabricLoader.getInstance().isDevelopmentEnvironment();
  public int maxHistoryCount = 50;

  public boolean enableMoveCommand = true;
  public boolean enableStackCommand = true;
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
