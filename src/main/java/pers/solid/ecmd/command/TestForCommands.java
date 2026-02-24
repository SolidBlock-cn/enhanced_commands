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
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final LiteralArgumentBuilder<CommandSourceStack> literal = ModCommands.literalR2("testfor");
    TestForBiomeCommand.INSTANCE.addArguments(literal, commandBuildContext, environment);
    TestForBlockCommand.INSTANCE.addArguments(literal, commandBuildContext, environment);
    TestForBlocksCommand.INSTANCE.addArguments(literal, commandBuildContext, environment);
    TestForBlockInfoCommand.INSTANCE.addArguments(literal, commandBuildContext, environment);
    TestForBlocksCommand.INSTANCE.addArguments(literal, commandBuildContext, environment);
    TestForEntityCommand.INSTANCE.addArguments(literal, commandBuildContext, environment);
    dispatcher.register(literal);
  }

  public interface Entry {
    void addArguments(LiteralArgumentBuilder<CommandSourceStack> testForBuilder, CommandBuildContext commandBuildContext, Commands.CommandSelection environment);
  }
}
