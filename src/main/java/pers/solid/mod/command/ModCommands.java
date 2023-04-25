package pers.solid.mod.command;

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
    GameModeAliasCommand.register(dispatcher);
    SeparatedExecuteCommand.register(dispatcher, registryAccess);
    TestForCommand.register(dispatcher, registryAccess);
  }
}
