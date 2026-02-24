package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.util.TextUtil;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public enum DebugIgnoreBoundaryCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {
    dispatcher.register(literal("debug:ignoreboundary")
        .executes(context -> {
          context.getSource().sendFeedback$ecBridge(() -> Component.literal("ignore boundary: ").append(TextUtil.wrapBoolean(DebugConfig.current.ignoreBoundary)), false);
          return DebugConfig.current.ignoreBoundary ? 1 : 0;
        })
        .then(argument("value", bool())
            .executes(context -> {
              final boolean value = getBool(context, "value");
              DebugConfig.current.ignoreBoundary = value;
              context.getSource().sendFeedback$ecBridge(() -> Component.literal("ignore boundary set to ").append(TextUtil.wrapBoolean(value)), true);
              return value ? 1 : 0;
            })));
    dispatcher.register(literal("debug:ignoreborder")
        .executes(context -> {
          context.getSource().sendFeedback$ecBridge(() -> Component.literal("ignore boundary: ").append(TextUtil.wrapBoolean(DebugConfig.current.ignoreBorder)), false);
          return DebugConfig.current.ignoreBorder ? 1 : 0;
        })
        .then(argument("value", bool())
            .executes(context -> {
              final boolean value = getBool(context, "value");
              DebugConfig.current.ignoreBorder = value;
              context.getSource().sendFeedback$ecBridge(() -> Component.literal("ignore boundary set to ").append(TextUtil.wrapBoolean(value)), true);
              return value ? 1 : 0;
            })));
  }
}
