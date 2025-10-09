package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockRotationArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.ModTrackedData;
import pers.solid.ecmd.argument.AxisArgument;
import pers.solid.ecmd.argument.DirectionArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.enums.CommandEnumType;

import java.util.Optional;
import java.util.function.BiFunction;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.DirectionArgumentType.direction;
import static pers.solid.ecmd.argument.DirectionArgumentType.getDirection;
import static pers.solid.ecmd.argument.SimpleEnumArgumentType.*;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum ActiveRegionCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final SimpleCommandExceptionType UNSUPPORTED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.commands.activeregion.unsupported"));
  public static final DynamicCommandExceptionType UNSUPPORTED_WITH_REASON = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.activeregion.unsupported_with_region", o));

  public static int executeGet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
    final Region region = player.getActiveRegionOrThrow$ec().region();
    if (region == null) {
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.activeregion.get_none", TextUtil.styled(player.getDisplayName(), Styles.TARGET)), false);
      return 0;
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.activeregion.get", TextUtil.styled(player.getDisplayName(), Styles.TARGET), TextUtil.literal(region).styled(Styles.RESULT)), false);
      return 1;
    }
  }

  public static int executeSet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final PlayerEntity player = source.getPlayerOrThrow();
    source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.activeregion.set.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET)), true);
    return 1;
  }

  public static int executeRemove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final PlayerEntity player = source.getPlayerOrThrow();
    player.setActiveRegion$ec(null);
    source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.activeregion.remove.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET)), true);
    return 1;
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
    return executeRegionModification(region -> region.moved(Vec3d.of(direction.getVector()).multiply(offset)), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.move.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(region.region()).styled(Styles.RESULT)), context);
  }

  public static int executeMoveVector(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMoveVector(getDouble(context, "x"), getDouble(context, "y"), getDouble(context, "z"), context);
  }

  public static int executeMoveVector(double x, double y, double z, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMoveVector(new Vec3d(x, y, z), context);
  }

  public static int executeMoveVector(Vec3d vec3d, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(vec3d), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.move.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(region.region()).styled(Styles.RESULT)), context);
  }

  public static int executeRotate(Vec3d pivot, BlockRotation blockRotation, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.rotated(blockRotation, pivot), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.rotate.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(region.region()).styled(Styles.RESULT)), context);
  }

  public static int executeMirror(Vec3d pivot, Direction.Axis axis, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.mirrored(axis, pivot), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.mirror.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(region.region()).styled(Styles.RESULT)), context);
  }

  public static int executeExpandDirection(double offset, Direction direction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset, direction), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.expand.direction.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(offset), TextUtil.wrapDirection(direction), TextUtil.literal(region.region()).styled(Styles.RESULT)), context);
  }

  public static int executeExpandAxis(double offset, Direction.Axis axis, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset, axis), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.expand.axis.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(offset), TextUtil.wrapAxis(axis), TextUtil.literal(region.region()).styled(Styles.RESULT)), context);
  }

  public static int executeExpandDirectionType(double offset, Direction.Type type, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final MutableText adverb = Text.translatable("enhanced_commands.direction_type." + type.name().toLowerCase() + ".adverb");
    return executeRegionModification(region -> region.expanded(offset, type), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.expand.axis.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(offset), adverb, TextUtil.literal(region.region()).styled(Styles.RESULT)), context);
  }

  public static int executeExpandAllDirections(double offset, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.expand.all_directions.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(offset), TextUtil.literal(region.region()).styled(Styles.RESULT)), context);
  }

  public static int executeRegionModification(FailableFunction<RegionSelection, RegionSelection, CommandSyntaxException> regionOperation, BiFunction<ServerPlayerEntity, RegionSelection, Text> messageSingle, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final ServerPlayerEntity player = source.getPlayerOrThrow();
    final RegionSelection operatedRegion = invokeOperationOrThrow(regionOperation, player.getActiveRegion$ec());

    // 注意：当玩家有 regionBuilder 时，会自动生成 region，且理论上 regionBuilder 和 region 进行的操作应当是一致的。
    player.getDataTracker().set(ModTrackedData.PLAYER_REGION_SELECTION, Optional.ofNullable(operatedRegion), true);
    source.sendFeedback$ecBridge(() -> messageSingle.apply(player, operatedRegion), true);
    return 1;
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(literalR2("activeregion")
        .then(literal("get")
            .executes(ActiveRegionCommand::executeGet))
        .then(literal("remove")
            .executes(ActiveRegionCommand::executeRemove))
        .then(literal("move")
            .executes(context -> executeMoveDirection(1, DirectionArgument.FRONT.apply(context.getSource()), context))
            .then(argument("amount", doubleArg())
                .executes(context -> executeMoveDirection(getDouble(context, "amount"), DirectionArgument.FRONT.apply(context.getSource()), context))
                .then(argument("direction", direction())
                    .executes(context -> executeMoveDirection(getDouble(context, "amount"), getDirection(context, "direction"), context))))
            .then(argument("x", doubleArg())
                .then(argument("y", doubleArg())
                    .then(argument("z", doubleArg())
                        .executes(ActiveRegionCommand::executeMoveVector)))))
        .then(literal("rotate")
            .then(argument("rotation", BlockRotationArgumentType.blockRotation())
                .executes(context -> executeRotate(EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER.toAbsolutePos(context.getSource()), BlockRotationArgumentType.getBlockRotation(context, "rotation"), context))
                .then(argument("pivot", EnhancedPosArgumentType.posPreferringCenteredInt())
                    .executes(context -> executeRotate(EnhancedPosArgumentType.getPos(context, "pivot"), BlockRotationArgumentType.getBlockRotation(context, "rotation"), context)))))
        .then(literal("mirror")
            .executes(context -> executeMirror(EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER.toAbsolutePos(context.getSource()), AxisArgument.FRONT_BACK.apply(context.getSource()), context))
            .then(argument("axis", axis(false))
                .executes(context -> executeMirror(EnhancedPosArgumentType.CURRENT_BLOCK_POS_CENTER.toAbsolutePos(context.getSource()), getAxis(context, "axis"), context))
                .then(argument("pivot", EnhancedPosArgumentType.posPreferringCenteredInt())
                    .executes(context -> executeMirror(EnhancedPosArgumentType.getPos(context, "pivot"), getAxis(context, "axis"), context)))))
        .then(literal("expand")
            .executes(context -> executeExpandDirection(1, DirectionArgument.FRONT.apply(context.getSource()), context))
            .then(argument("offset", doubleArg())
                .executes(context -> executeExpandDirection(getDouble(context, "offset"), DirectionArgument.FRONT.apply(context.getSource()), context))
                .then(argument("direction", direction())
                    .executes(context -> executeExpandDirection(getDouble(context, "offset"), getDirection(context, "direction"), context)))
                .then(argument("axis", axis(true))
                    .executes(context -> executeExpandAxis(getDouble(context, "offset"), getAxis(context, "axis"), context)))
                .then(argument("direction_type", simpleEnum(CommandEnumType.DIRECTION_TYPE))
                    .executes(context -> executeExpandDirectionType(getDouble(context, "offset"), context.getArgument("direction_type", Direction.Type.class), context)))
                .then(literal("all")
                    .executes(context -> executeExpandAllDirections(getDouble(context, "offset"), context))))
            .then(argument("direction", direction())
                .executes(context -> executeExpandDirection(1, getDirection(context, "direction"), context)))
            .then(argument("axis", axis(true))
                .executes(context -> executeExpandAxis(1, getAxis(context, "axis"), context)))
            .then(argument("direction_type", simpleEnum(CommandEnumType.DIRECTION_TYPE))
                .executes(context -> executeExpandDirectionType(1, context.getArgument("direction_type", Direction.Type.class), context)))
            .then(literal("all")
                .executes(context -> executeExpandAllDirections(1, context)))));
    dispatcher.register(literalR2("ar").redirect(literalCommandNode));
  }
}
