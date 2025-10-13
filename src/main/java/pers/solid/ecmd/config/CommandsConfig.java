package pers.solid.ecmd.config;

import net.fabricmc.loader.api.FabricLoader;

@ConfigEntry.NoDescription
public class CommandsConfig implements Cloneable {
  public static final CommandsConfig DEFAULT = new CommandsConfig();
  public static CommandsConfig current = DEFAULT;

  public boolean enableDebugCommands = FabricLoader.getInstance().isDevelopmentEnvironment();

  @ConfigEntry.NoDescription
  public boolean enableMoveCommand = true;

  @ConfigEntry.NoDescription
  public boolean enableStackCommand = true;

  @ConfigEntry.NoDescription
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
