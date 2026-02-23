package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

import java.util.Collection;
import java.util.Collections;

/**
 * @see net.minecraft.server.commands.GameModeCommand
 */
public enum GameModeAliasCommand implements CommandRegistrationCallback {
  INSTANCE;

  private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String literalName, GameType gameMode) {
    dispatcher.register(Commands.literal(literalName)
        .requires(ModCommands.REQUIRES_PERMISSION_2)
        .executes(context -> execute(context, Collections.singleton(context.getSource().getPlayerOrException()), gameMode))
        .then(Commands.argument("target", EntityArgument.players())
            .executes(context -> execute(context, EntityArgument.getPlayers(context, "target"), gameMode))));
  }

  public static MutableComponent getName(GameType gameMode) {
    return Component.translatable("gameMode." + gameMode.getName());
  }

  private static void sendFeedback(CommandSourceStack source, ServerPlayer player, GameType gameMode) {
    final Component name = getName(gameMode);
    if (source.getEntity() == player) {
      source.sendFeedback$ecBridge(() -> Component.translatable("commands.gamemode.success.self", name), true);
    } else {
      if (source.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
        player.sendSystemMessage(Component.translatable("gameMode.changed", name));
      }

      source.sendFeedback$ecBridge(() -> Component.translatable("commands.gamemode.success.other", player.getDisplayName(), name), true);
    }
  }

  private static int execute(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, GameType gameMode) {
    int count = 0;
    for (ServerPlayer serverPlayerEntity : targets) {
      if (serverPlayerEntity.setGameMode(gameMode)) {
        sendFeedback(context.getSource(), serverPlayerEntity, gameMode);
        count++;
      }
    }
    return count;
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    register(dispatcher, "gmc", GameType.CREATIVE);
    register(dispatcher, "gms", GameType.SURVIVAL);
    register(dispatcher, "gma", GameType.ADVENTURE);
    register(dispatcher, "gmsp", GameType.SPECTATOR);
  }
}
