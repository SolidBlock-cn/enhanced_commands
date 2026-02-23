package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public enum TestForCommands implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    final LiteralArgumentBuilder<CommandSourceStack> literal = ModCommands.literalR2("testfor");
    TestForBiomeCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    TestForBlockCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    TestForBlocksCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    TestForBlockInfoCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    TestForBlocksCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    TestForEntityCommand.INSTANCE.addArguments(literal, registryAccess, environment);
    dispatcher.register(literal);
  }

  public interface Entry {
    void addArguments(LiteralArgumentBuilder<CommandSourceStack> testForBuilder, CommandBuildContext registryAccess, Commands.CommandSelection environment);
  }
}
