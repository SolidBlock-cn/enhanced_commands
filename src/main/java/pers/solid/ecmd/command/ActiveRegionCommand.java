package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.command.argument.BlockRotationArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.EnhancedRedirectModifier;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum ActiveRegionCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.keywordArgsBuilder()
      .addOptionalArg("fixed", BoolArgumentType.bool(), true)
      .build();

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final ArgumentCommandNode<ServerCommandSource, EntitySelector> playersArgumentNode;
    final LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(literal("activeregion")
        .then(playersArgumentNode = argument("players", EntityArgumentType.players())
            .then(literal("get")
                .executes(context -> executeGet(EntityArgumentType.getPlayer(context, "players"), context)))
            .then(literal("set")
                .then(argument("region", RegionArgumentType.region(registryAccess))
                    .executes(context -> executeSet(EntityArgumentType.getPlayers(context, "players"), context, true))
                    .then(argument("keyword_args", KEYWORD_ARGS)
                        .executes(context -> executeSet(EntityArgumentType.getPlayers(context, "players"), context, KeywordArgsArgumentType.getKeywordArgs("keyword_args", context).getBoolean("fixed"))))))
            .then(literal("remove")
                .executes(context -> executeRemove(EntityArgumentType.getPlayers(context, "players"), context)))
            .then(literal("move")
                .executes(context -> executeMoveDirection(1, DirectionArgument.FRONT.apply(context.getSource()), context))
                .then(argument("amount", DoubleArgumentType.doubleArg())
                    .executes(context -> executeMoveDirection(DoubleArgumentType.getDouble(context, "amount"), DirectionArgument.FRONT.apply(context.getSource()), context))
                    .then(argument("direction", DirectionArgumentType.create())
                        .executes(context -> executeMoveDirection(DoubleArgumentType.getDouble(context, "amount"), DirectionArgumentType.getDirection(context, "direction"), context))))
                .then(argument("x", DoubleArgumentType.doubleArg())
                    .then(argument("y", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                            .executes(ActiveRegionCommand::executeMoveVector)))))
            .then(literal("rotate")
                .then(argument("rotation", BlockRotationArgumentType.blockRotation())
                    .executes(context -> executeRotate(EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER.toAbsolutePos(context.getSource()), BlockRotationArgumentType.getBlockRotation(context, "rotation"), context))
                    .then(argument("pivot", EnhancedPosArgumentType.posPreferringCenteredInt())
                        .executes(context -> executeRotate(EnhancedPosArgumentType.getPos(context, "pivot"), BlockRotationArgumentType.getBlockRotation(context, "rotation"), context)))))
            .then(literal("mirror")
                .executes(context -> executeMirror(EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER.toAbsolutePos(context.getSource()), DirectionArgument.FRONT.apply(context.getSource()).getAxis(), context)))
            .build())
    );
    dispatcher.register(literal("ar").redirect(literalCommandNode));

    final EnhancedRedirectModifier<ServerCommandSource> slashModifier = EnhancedRedirectModifier.of((arguments, source) -> arguments.put("players", new ParsedArgument<>(0, 0, new EntitySelectorReader(new StringReader("@s")).read())));
    final Command<ServerCommandSource> slashExecution = context -> executeGet(context.getSource().getPlayerOrThrow(), context);
    dispatcher.register(literal("/activeregion")
        .forward(playersArgumentNode, slashModifier, false)
        .executes(slashExecution));
    dispatcher.register(literal("/ar")
        .forward(playersArgumentNode, slashModifier, false)
        .executes(slashExecution));
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
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.set.single", players.iterator().next().getName().copy().styled(TextUtil.STYLE_FOR_TARGET)), true);
    } else {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.set.multiple", Integer.toString(players.size())), false);
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
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.remove.single", players.iterator().next().getName().copy().styled(TextUtil.STYLE_FOR_TARGET)), true);
    } else {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.remove.multiple", Integer.toString(players.size())), false);
    }
    return successes;
  }

  public static final DynamicCommandExceptionType NO_ACTIVE_REGION = ModCommands.PLAYER_HAS_NO_ACTIVE_REGION;
  public static final SimpleCommandExceptionType UNSUPPORTED = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.commands.activeregion.unsupported"));
  public static final DynamicCommandExceptionType UNSUPPORTED_WITH_REASON = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.commands.activeregion.unsupported_with_region", o));

  public static @NotNull RegionArgument<?> getActiveRegionArgumentOrThrow(@NotNull ServerPlayerEntity serverPlayerEntity) throws CommandSyntaxException {
    final RegionArgument<?> regionArgument = ((ServerPlayerEntityExtension) serverPlayerEntity).ec_getActiveRegion();
    if (regionArgument == null) {
      throw NO_ACTIVE_REGION.create(serverPlayerEntity.getName());
    }
    return regionArgument;
  }

  public static @NotNull Region getActiveAbsoluteRegionOrThrow(@NotNull ServerPlayerEntity serverPlayerEntity, ServerCommandSource source) throws CommandSyntaxException {
    return getActiveRegionArgumentOrThrow(serverPlayerEntity).toAbsoluteRegion(source);
  }

  public static <T, R> R invokeOperationOrThrow(FailableFunction<T, R, CommandSyntaxException> function, T input) throws CommandSyntaxException {
    try {
      return function.apply(input);
    } catch (UnsupportedOperationException e) {
      if (e.getCause() instanceof CommandSyntaxException c) {
        throw UNSUPPORTED_WITH_REASON.create(c.getRawMessage());
      } else {
        throw UNSUPPORTED.create();
      }
    }
  }

  public static int executeMoveDirection(double offset, Direction direction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(offset, direction), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.move.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), Text.literal(region.asString()).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.move.multiple", value), context);
  }

  public static int executeMoveVector(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMoveVector(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z"), context);
  }

  public static int executeMoveVector(double x, double y, double z, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMoveVector(new Vec3d(x, y, z), context);
  }

  public static int executeMoveVector(Vec3d vec3d, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(vec3d), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.move.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), Text.literal(region.asString()).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.move.multiple", value), context);
  }

  public static int executeRotate(Vec3d pivot, BlockRotation blockRotation, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.rotated(pivot, blockRotation), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.rotate.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), Text.literal(region.asString()).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.rotate.multiple", value), context);
  }

  public static int executeMirror(Vec3d pivot, Direction.Axis axis, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.mirrored(pivot, axis), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.mirror.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), Text.literal(region.asString()).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.mirror.multiple", value), context);
  }

  public static int executeRegionModification(FailableFunction<Region, Region, CommandSyntaxException> regionOperation, BiFunction<ServerPlayerEntity, Region, Text> messageSingle, IntFunction<Text> messageMultiple, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
    final ServerCommandSource source = context.getSource();
    if (players.size() == 1) {
      final ServerPlayerEntity player = players.iterator().next();
      final Region operatedRegion = invokeOperationOrThrow(regionOperation, getActiveAbsoluteRegionOrThrow(player, source));
      ((ServerPlayerEntityExtension) player).ec_setActiveRegion(operatedRegion);
      CommandBridge.sendFeedback(source, () -> messageSingle.apply(player, operatedRegion), true);
      return 1;
    } else {
      int successes = 0;
      for (ServerPlayerEntity player : players) {
        try {
          final Region movedRegion = invokeOperationOrThrow(regionOperation, getActiveAbsoluteRegionOrThrow(player, source));
          ((ServerPlayerEntityExtension) player).ec_setActiveRegion(movedRegion);
          successes++;
        } catch (CommandSyntaxException ignored) {}
      }
      int finalSuccesses = successes;
      CommandBridge.sendFeedback(source, () -> messageMultiple.apply(finalSuccesses), true);
      return successes;
    }
  }
}
