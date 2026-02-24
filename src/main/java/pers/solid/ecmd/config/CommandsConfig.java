package pers.solid.ecmd.config;

import net.fabricmc.loader.api.FabricLoader;

public class CommandsConfig {
  public static final CommandsConfig DEFAULT = new CommandsConfig();
  public static CommandsConfig CURRENT = DEFAULT;

  public boolean enableDebugCommands = FabricLoader.getInstance().isDevelopmentEnvironment();
  public int maxHistoryCount = 50;
}
