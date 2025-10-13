package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.util.TextUtil;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum DebugIgnoreBoundaryCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
    dispatcher.register(literal("debug:ignoreboundary")
        .executes(context -> {
          context.getSource().sendFeedback$ecBridge(() -> Text.literal("ignore boundary: ").append(TextUtil.wrapBoolean(DebugConfig.current.ignoreBoundary)), false);
          return DebugConfig.current.ignoreBoundary ? 1 : 0;
        })
        .then(argument("value", bool())
            .executes(context -> {
              final boolean value = getBool(context, "value");
              DebugConfig.current.ignoreBoundary = value;
              context.getSource().sendFeedback$ecBridge(() -> Text.literal("ignore boundary set to ").append(TextUtil.wrapBoolean(value)), true);
              return value ? 1 : 0;
            })));
    dispatcher.register(literal("debug:ignoreborder")
        .executes(context -> {
          context.getSource().sendFeedback$ecBridge(() -> Text.literal("ignore boundary: ").append(TextUtil.wrapBoolean(DebugConfig.current.ignoreBorder)), false);
          return DebugConfig.current.ignoreBorder ? 1 : 0;
        })
        .then(argument("value", bool())
            .executes(context -> {
              final boolean value = getBool(context, "value");
              DebugConfig.current.ignoreBorder = value;
              context.getSource().sendFeedback$ecBridge(() -> Text.literal("ignore boundary set to ").append(TextUtil.wrapBoolean(value)), true);
              return value ? 1 : 0;
            })));
  }
}
