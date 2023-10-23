package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.argument.RegionArgumentType;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.Collection;
import java.util.Collections;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum ActiveRegionCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.keywordArgsBuilder()
      .addOptionalArg("fixed", BoolArgumentType.bool(), true)
      .build();

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(literal("activeregion")
        .then(literal("get")
            .executes(context -> executeGet(context.getSource().getPlayerOrThrow(), context))
            .then(argument("player", EntityArgumentType.player())
                .executes(context -> executeGet(EntityArgumentType.getPlayer(context, "player"), context))))
        .then(literal("set")
            .then(argument("players", EntityArgumentType.players())
                .then(argument("region", RegionArgumentType.region(registryAccess))
                    .executes(context -> executeSet(EntityArgumentType.getPlayers(context, "players"), context, true))
                    .then(argument("keyword_args", KEYWORD_ARGS)
                        .executes(context -> executeSet(EntityArgumentType.getPlayers(context, "players"), context, KeywordArgsArgumentType.getKeywordArgs("keyword_args", context).getBoolean("fixed")))))))
        .then(literal("remove")
            .executes(context -> executeRemove(Collections.singleton(context.getSource().getPlayerOrThrow()), context))
            .then(argument("players", EntityArgumentType.players())
                .executes(context -> executeRemove(EntityArgumentType.getPlayers(context, "players"), context))))
        .then(argument("region", RegionArgumentType.region(registryAccess))
            .executes(context -> executeSet(Collections.singleton(context.getSource().getPlayer()), context, true))
            .then(argument("keyword_args", KEYWORD_ARGS)
                .executes(context -> executeSet(Collections.singleton(context.getSource().getPlayer()), context, KeywordArgsArgumentType.getKeywordArgs("keyword_args", context).getBoolean("fixed")))))
    );
    dispatcher.register(literal("ar").redirect(literalCommandNode));
  }

  public static int executeGet(ServerPlayerEntity player, CommandContext<ServerCommandSource> context) {
    final RegionArgument<?> regionArgument = ((ServerPlayerEntityExtension) player).ec_getActiveRegion();
    if (regionArgument == null) {
      CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhancedCommands.commands.activeregion.get_none", player.getName().copy().styled(TextUtil.STYLE_FOR_TARGET)), true);
      return 0;
    } else {
      CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhancedCommands.commands.activeregion.get", player.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), Text.literal(regionArgument.toAbsoluteRegion(player.getCommandSource()).asString()).styled(TextUtil.STYLE_FOR_ACTUAL)), true);
      return 1;
    }
  }

  public static int executeSet(Collection<ServerPlayerEntity> players, CommandContext<ServerCommandSource> context, boolean fixed) {
    RegionArgument<?> region = context.getArgument("region", RegionArgument.class);
    int successes = 0;
    final ServerCommandSource source = context.getSource();
    for (ServerPlayerEntity player : players) {
      if (fixed) {
        final Region absoluteRegion = region.toAbsoluteRegion(source);
        region = __ -> absoluteRegion;
      }
      ((ServerPlayerEntityExtension) player).ec_setActiveRegion(region);
      successes++;
    }
    if (players.size() == 1) {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.set_single", players.iterator().next().getName().copy().styled(TextUtil.STYLE_FOR_TARGET)), true);
    } else {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.set_multiple", Integer.toString(players.size())), false);
    }
    return successes;
  }

  public static int executeRemove(Collection<ServerPlayerEntity> players, CommandContext<ServerCommandSource> context) {
    int successes = 0;
    final ServerCommandSource source = context.getSource();
    for (ServerPlayerEntity player : players) {
      ((ServerPlayerEntityExtension) player).ec_setActiveRegion(null);
      successes++;
    }
    if (players.size() == 1) {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.remove_single", players.iterator().next().getName().copy().styled(TextUtil.STYLE_FOR_TARGET)), true);
    } else {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.remove_multiple", Integer.toString(players.size())), false);
    }
    return successes;
  }
}
