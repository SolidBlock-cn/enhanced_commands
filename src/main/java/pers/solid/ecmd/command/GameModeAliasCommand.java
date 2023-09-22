package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Collection;
import java.util.Collections;

/**
 * @see net.minecraft.server.command.GameModeCommand
 */
public enum GameModeAliasCommand implements CommandRegistrationCallback {
  INSTANCE;

  private static void register(CommandDispatcher<ServerCommandSource> dispatcher, String literalName, GameMode gameMode) {
    dispatcher.register(CommandManager.literal(literalName)
        .requires(ModCommands.REQUIRES_PERMISSION_2)
        .executes(context -> execute(context, Collections.singleton(context.getSource().getPlayerOrThrow()), gameMode))
        .then(CommandManager.argument("target", EntityArgumentType.players())
            .executes(context -> execute(context, EntityArgumentType.getPlayers(context, "target"), gameMode))));
  }

  public static MutableText getName(GameMode gameMode) {
    return Text.translatable("gameMode." + gameMode.getName());
  }

  private static void sendFeedback(ServerCommandSource source, ServerPlayerEntity player, GameMode gameMode) {
    final Text name = getName(gameMode);
    if (source.getEntity() == player) {
      CommandBridge.sendFeedback(source, () -> Text.translatable("commands.gamemode.success.self", name), true);
    } else {
      if (source.getWorld().getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
        player.sendMessage(Text.translatable("gameMode.changed", name));
      }

      CommandBridge.sendFeedback(source, () -> Text.translatable("commands.gamemode.success.other", player.getDisplayName(), name), true);
    }
  }

  private static int execute(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> targets, GameMode gameMode) {
    int count = 0;
    for (ServerPlayerEntity serverPlayerEntity : targets) {
      if (serverPlayerEntity.changeGameMode(gameMode)) {
        sendFeedback(context.getSource(), serverPlayerEntity, gameMode);
        count++;
      }
    }
    return count;
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    register(dispatcher, "gmc", GameMode.CREATIVE);
    register(dispatcher, "gms", GameMode.SURVIVAL);
    register(dispatcher, "gma", GameMode.ADVENTURE);
    register(dispatcher, "gmsp", GameMode.SPECTATOR);
  }
}
