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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.EnhancedRedirectModifier;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum ActiveRegionCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("fixed", BoolArgumentType.bool(), true)
      .build();

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final ArgumentCommandNode<ServerCommandSource, EntitySelector> playersArgumentNode;
    final LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(literalR2("activeregion")
        .then(playersArgumentNode = argument("players", EntityArgumentType.players())
            .then(literal("get")
                .executes(context -> executeGet(EntityArgumentType.getPlayer(context, "players"), context)))
            .then(literal("set")
                .then(argument("region", RegionArgumentType.region(registryAccess))
                    .executes(context -> executeSet(EntityArgumentType.getPlayers(context, "players"), context, true))
                    .then(argument("keyword_args", KEYWORD_ARGS)
                        .executes(context -> executeSet(EntityArgumentType.getPlayers(context, "players"), context, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args").getBoolean("fixed"))))))
            .then(literal("remove")
                .executes(context -> executeRemove(EntityArgumentType.getPlayers(context, "players"), context)))
            .then(literal("move")
                .executes(context -> executeMoveDirection(1, DirectionArgument.FRONT.apply(context.getSource()), context))
                .then(argument("amount", DoubleArgumentType.doubleArg())
                    .executes(context -> executeMoveDirection(DoubleArgumentType.getDouble(context, "amount"), DirectionArgument.FRONT.apply(context.getSource()), context))
                    .then(argument("direction", DirectionArgumentType.direction())
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
                .executes(context -> executeMirror(EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER.toAbsolutePos(context.getSource()), AxisArgument.FRONT_BACK.apply(context.getSource()), context))
                .then(argument("axis", AxisArgumentType.axis(false))
                    .executes(context -> executeMirror(EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER.toAbsolutePos(context.getSource()), AxisArgumentType.getAxis(context, "axis"), context))
                    .then(argument("pivot", EnhancedPosArgumentType.posPreferringCenteredInt())
                        .executes(context -> executeMirror(EnhancedPosArgumentType.getPos(context, "pivot"), AxisArgumentType.getAxis(context, "axis"), context)))))
            .then(literal("expand")
                .executes(context -> executeExpandDirection(1, DirectionArgument.FRONT.apply(context.getSource()), context))
                .then(argument("direction", DirectionArgumentType.direction())
                    .executes(context -> executeExpandDirection(1, DirectionArgumentType.getDirection(context, "direction"), context))
                    .then(argument("offset", DoubleArgumentType.doubleArg())
                        .executes(context -> executeExpandDirection(DoubleArgumentType.getDouble(context, "offset"), DirectionArgumentType.getDirection(context, "direction"), context))))
                .then(argument("axis", AxisArgumentType.axis(true))
                    .executes(context -> executeExpandAxis(1, AxisArgumentType.getAxis(context, "axis"), context))
                    .then(argument("offset", DoubleArgumentType.doubleArg())
                        .executes(context -> executeExpandAxis(DoubleArgumentType.getDouble(context, "offset"), AxisArgumentType.getAxis(context, "axis"), context))))
                .then(argument("direction_type", new SimpleEnumArgumentTypes.DirectionTypeArgumentType())
                    .executes(context -> executeExpandDirectionType(1, context.getArgument("direction_type", Direction.Type.class), context))
                    .then(argument("offset", DoubleArgumentType.doubleArg())
                        .executes(context -> executeExpandDirectionType(DoubleArgumentType.getDouble(context, "offset"), context.getArgument("direction_type", Direction.Type.class), context))))
                .then(literal("all")
                    .executes(context -> executeExpandAllDirections(1, context))
                    .then(argument("offset", DoubleArgumentType.doubleArg())
                        .executes(context -> executeExpandAllDirections(DoubleArgumentType.getDouble(context, "offset"), context)))))
            .build())
    );
    dispatcher.register(literal("ar").redirect(literalCommandNode));

    final EnhancedRedirectModifier.Constant<ServerCommandSource> slashModifier = (arguments, previousArguments, source) -> arguments.put("players", new ParsedArgument<>(0, 0, new EntitySelectorReader(new StringReader("@s")).read()));
    final Command<ServerCommandSource> slashExecution = context -> executeGet(context.getSource().getPlayerOrThrow(), context);
    dispatcher.register(literal("/activeregion")
        .forward(playersArgumentNode, slashModifier, false)
        .executes(slashExecution));
    dispatcher.register(literal("/ar")
        .forward(playersArgumentNode, slashModifier, false)
        .executes(slashExecution));
  }

  public static int executeGet(ServerPlayerEntity player, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final Region region = ((ServerPlayerEntityExtension) player).ec$getOrEvaluateActiveRegion(context.getSource());
    if (region == null) {
      CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhancedCommands.commands.activeregion.get_none", player.getName().copy().styled(TextUtil.STYLE_FOR_TARGET)), true);
      return 0;
    } else {
      CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhancedCommands.commands.activeregion.get", player.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), true);
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
      ((ServerPlayerEntityExtension) player).ec$setActiveRegion(region);
      ((ServerPlayerEntityExtension) player).ec$setRegionBuilder(null);
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
      ((ServerPlayerEntityExtension) player).ec$setActiveRegion(null);
      ((ServerPlayerEntityExtension) player).ec$setRegionBuilder(null);
      successes++;
    }
    if (players.size() == 1) {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.remove.single", players.iterator().next().getName().copy().styled(TextUtil.STYLE_FOR_TARGET)), true);
    } else {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.activeregion.remove.multiple", Integer.toString(players.size())), false);
    }
    return successes;
  }

  public static final DynamicCommandExceptionType NO_ACTIVE_REGION = ServerPlayerEntityExtension.PLAYER_HAS_NO_ACTIVE_REGION;
  public static final SimpleCommandExceptionType UNSUPPORTED = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.commands.activeregion.unsupported"));
  public static final DynamicCommandExceptionType UNSUPPORTED_WITH_REASON = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.commands.activeregion.unsupported_with_region", o));

  public static @NotNull RegionArgument<?> getActiveRegionArgumentOrThrow(@NotNull ServerPlayerEntity serverPlayerEntity, ServerCommandSource source) throws CommandSyntaxException {
    final RegionArgument<?> regionArgument = ((ServerPlayerEntityExtension) serverPlayerEntity).ec$getOrEvaluateActiveRegion(source);
    if (regionArgument == null) {
      throw NO_ACTIVE_REGION.create(serverPlayerEntity.getName());
    }
    return regionArgument;
  }

  public static @NotNull Region getActiveAbsoluteRegionOrThrow(@NotNull ServerPlayerEntity serverPlayerEntity, ServerCommandSource source) throws CommandSyntaxException {
    return getActiveRegionArgumentOrThrow(serverPlayerEntity, source).toAbsoluteRegion(source);
  }

  public static <T, R> R invokeOperationOrThrow(FailableFunction<T, R, CommandSyntaxException> function, T input) throws CommandSyntaxException {
    try {
      return function.apply(input);
    } catch (Throwable e) {
      if (e.getCause() instanceof CommandSyntaxException c) {
        throw UNSUPPORTED_WITH_REASON.create(c.getRawMessage());
      } else {
        throw UNSUPPORTED.create();
      }
    }
  }

  public static int executeMoveDirection(double offset, Direction direction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(Vec3d.of(direction.getVector()).multiply(offset)), rb -> rb.move(Vec3d.of(direction.getVector()).multiply(offset)), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.move.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.move.multiple", value), context);
  }

  public static int executeMoveVector(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMoveVector(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z"), context);
  }

  public static int executeMoveVector(double x, double y, double z, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMoveVector(new Vec3d(x, y, z), context);
  }

  public static int executeMoveVector(Vec3d vec3d, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(vec3d), rb -> rb.move(vec3d), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.move.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.move.multiple", value), context);
  }

  public static int executeRotate(Vec3d pivot, BlockRotation blockRotation, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.rotated(blockRotation, pivot), rb -> rb.rotate(blockRotation, pivot), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.rotate.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.rotate.multiple", value), context);
  }

  public static int executeMirror(Vec3d pivot, Direction.Axis axis, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.mirrored(axis, pivot), rb -> rb.mirror(axis, pivot), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.mirror.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.mirror.multiple", value), context);
  }


  public static int executeExpandDirection(double offset, Direction direction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset, direction), rb -> rb.expand(offset, direction), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.expand.direction.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(offset), TextUtil.wrapDirection(direction), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.expand.direction.single", value, TextUtil.literal(offset), TextUtil.wrapDirection(direction)), context);
  }

  public static int executeExpandAxis(double offset, Direction.Axis axis, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset, axis), rb -> rb.expand(offset, axis), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.expand.axis.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(offset), TextUtil.wrapAxis(axis), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.expand.axis.single", value, TextUtil.literal(offset), TextUtil.wrapAxis(axis)), context);
  }

  public static int executeExpandDirectionType(double offset, Direction.Type type, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final MutableText adverb = Text.translatable("enhancedCommands.direction_type." + type.name().toLowerCase() + ".adverb");
    return executeRegionModification(region -> region.expanded(offset, type), rb -> rb.expand(offset, type), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.expand.axis.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(offset), adverb, TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.expand.axis.single", value, TextUtil.literal(offset), adverb), context);
  }

  public static int executeExpandAllDirections(double offset, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset), rb -> rb.expand(offset), (serverPlayerEntity, region) -> Text.translatable("enhancedCommands.commands.activeregion.expand.all_directions.single", serverPlayerEntity.getName().copy().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(offset), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), value -> TextUtil.enhancedTranslatable("enhancedCommands.commands.activeregion.expand.all_directions.single", value, TextUtil.literal(offset)), context);
  }

  public static int executeRegionModification(FailableFunction<Region, Region, CommandSyntaxException> regionOperation, FailableConsumer<RegionBuilder, CommandSyntaxException> regionBuilderFunction, BiFunction<ServerPlayerEntity, Region, Text> messageSingle, IntFunction<Text> messageMultiple, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
    final ServerCommandSource source = context.getSource();
    if (players.size() == 1) {
      final ServerPlayerEntity player = players.iterator().next();
      final Region operatedRegion = invokeOperationOrThrow(regionOperation, getActiveAbsoluteRegionOrThrow(player, source));

      // 注意：当玩家有 regionBuilder 时，会自动生成 region，且理论上 regionBuilder 和 region 进行的操作应当是一致的。
      ((ServerPlayerEntityExtension) player).ec$setActiveRegion(operatedRegion);
      final RegionBuilder regionBuilder = ((ServerPlayerEntityExtension) player).ec$getRegionBuilder();
      if (regionBuilder != null) {
        invokeOperationOrThrow(input -> {
          regionBuilderFunction.accept(input);
          return null;
        }, regionBuilder);
      }
      CommandBridge.sendFeedback(source, () -> messageSingle.apply(player, operatedRegion), true);
      return 1;
    } else {
      int successes = 0;
      for (ServerPlayerEntity player : players) {
        try {
          final Region movedRegion = invokeOperationOrThrow(regionOperation, getActiveAbsoluteRegionOrThrow(player, source));
          ((ServerPlayerEntityExtension) player).ec$setActiveRegion(movedRegion);
          final RegionBuilder regionBuilder = ((ServerPlayerEntityExtension) player).ec$getRegionBuilder();
          if (regionBuilder != null) {
            invokeOperationOrThrow(input -> {
              regionBuilderFunction.accept(input);
              return null;
            }, regionBuilder);
          }
          successes++;
        } catch (CommandSyntaxException ignored) {}
      }
      int finalSuccesses = successes;
      CommandBridge.sendFeedback(source, () -> messageMultiple.apply(finalSuccesses), true);
      return successes;
    }
  }
}
