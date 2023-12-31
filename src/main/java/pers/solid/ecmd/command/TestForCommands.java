package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public enum TestForCommands implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final LiteralArgumentBuilder<ServerCommandSource> literal = ModCommands.literalR2("testfor");
    TestForBiomeCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    TestForBlockCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    TestForBlockInfoCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    TestForEntityCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    dispatcher.register(literal);
  }

  public interface Entry {
    void addArguments(LiteralArgumentBuilder<ServerCommandSource> testForBuilder, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment);
  }
}
