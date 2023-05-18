package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

public final class ModCommands implements CommandRegistrationCallback {
  public static final Predicate<ServerCommandSource> REQUIRES_PERMISSION_2 = source -> source.hasPermissionLevel(2);

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    GameModeAliasCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RandCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    SeparatedExecuteCommand.register(dispatcher, registryAccess);
    SetBlocksCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TasksCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestForCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestPosCommand.INSTANCE.register(dispatcher, registryAccess, environment);
  }
}
