package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;

public enum TestForCommands implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final LiteralArgumentBuilder<CommandSourceStack> literal = EnhancedCommandsCommands.literalR2("testfor");
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
