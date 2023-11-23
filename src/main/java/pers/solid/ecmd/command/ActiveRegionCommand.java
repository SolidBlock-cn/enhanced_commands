package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandException;
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
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.function.BiFunction;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum ActiveRegionCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(literalR2("activeregion")
        .then(literal("get")
            .executes(ActiveRegionCommand::executeGet))
        .then(literal("set")
            .then(argument("region", RegionArgumentType.region(registryAccess))
                .executes(ActiveRegionCommand::executeSet)))
        .then(literal("remove")
            .executes(ActiveRegionCommand::executeRemove))
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
    );
    dispatcher.register(literal("ar").redirect(literalCommandNode));
  }

  public static int executeGet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
    final Region region = ((ServerPlayerEntityExtension) player).ec$getOrEvaluateActiveRegion();
    if (region == null) {
      CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhanced_commands.commands.activeregion.get_none", TextUtil.styled(player.getDisplayName(), TextUtil.STYLE_FOR_TARGET)), false);
      return 0;
    } else {
      CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhanced_commands.commands.activeregion.get", TextUtil.styled(player.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), false);
      return 1;
    }
  }

  public static int executeSet(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final PlayerEntity player = source.getPlayerOrThrow();
    CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.activeregion.set.single", TextUtil.styled(player.getDisplayName(), TextUtil.STYLE_FOR_TARGET)), true);
    return 1;
  }

  public static int executeRemove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final PlayerEntity player = source.getPlayerOrThrow();
    CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.activeregion.remove.single", TextUtil.styled(player.getDisplayName(), TextUtil.STYLE_FOR_TARGET)), true);
    return 1;
  }

  public static final SimpleCommandExceptionType UNSUPPORTED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.commands.activeregion.unsupported"));
  public static final DynamicCommandExceptionType UNSUPPORTED_WITH_REASON = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.activeregion.unsupported_with_region", o));

  public static <T, R> R invokeOperationOrThrow(FailableFunction<T, R, CommandSyntaxException> function, T input) throws CommandSyntaxException {
    try {
      return function.apply(input);
    } catch (Throwable e) {
      if (e.getCause() instanceof CommandSyntaxException c) {
        throw UNSUPPORTED_WITH_REASON.create(c.getRawMessage());
      } else if (e.getCause() instanceof CommandException c) {
        throw UNSUPPORTED_WITH_REASON.create(c.getTextMessage());
      } else {
        throw UNSUPPORTED.create();
      }
    }
  }

  public static int executeMoveDirection(double offset, Direction direction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(Vec3d.of(direction.getVector()).multiply(offset)), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.move.single", TextUtil.styled(serverPlayerEntity.getName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), context);
  }

  public static int executeMoveVector(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMoveVector(DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z"), context);
  }

  public static int executeMoveVector(double x, double y, double z, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMoveVector(new Vec3d(x, y, z), context);
  }

  public static int executeMoveVector(Vec3d vec3d, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(vec3d), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.move.single", TextUtil.styled(serverPlayerEntity.getName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), context);
  }

  public static int executeRotate(Vec3d pivot, BlockRotation blockRotation, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.rotated(blockRotation, pivot), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.rotate.single", TextUtil.styled(serverPlayerEntity.getName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), context);
  }

  public static int executeMirror(Vec3d pivot, Direction.Axis axis, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.mirrored(axis, pivot), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.mirror.single", TextUtil.styled(serverPlayerEntity.getName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), context);
  }


  public static int executeExpandDirection(double offset, Direction direction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset, direction), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.expand.direction.single", TextUtil.styled(serverPlayerEntity.getName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(offset), TextUtil.wrapDirection(direction), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), context);
  }

  public static int executeExpandAxis(double offset, Direction.Axis axis, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset, axis), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.expand.axis.single", TextUtil.styled(serverPlayerEntity.getName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(offset), TextUtil.wrapAxis(axis), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), context);
  }

  public static int executeExpandDirectionType(double offset, Direction.Type type, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final MutableText adverb = Text.translatable("enhanced_commands.direction_type." + type.name().toLowerCase() + ".adverb");
    return executeRegionModification(region -> region.expanded(offset, type), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.expand.axis.single", TextUtil.styled(serverPlayerEntity.getName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(offset), adverb, TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), context);
  }

  public static int executeExpandAllDirections(double offset, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset), (serverPlayerEntity, region) -> Text.translatable("enhanced_commands.commands.activeregion.expand.all_directions.single", TextUtil.styled(serverPlayerEntity.getName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(offset), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_RESULT)), context);
  }

  public static int executeRegionModification(FailableFunction<Region, Region, CommandSyntaxException> regionOperation, BiFunction<ServerPlayerEntity, Region, Text> messageSingle, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final ServerPlayerEntity player = source.getPlayerOrThrow();
    final Region operatedRegion = invokeOperationOrThrow(regionOperation, ((ServerPlayerEntityExtension) player).ec$getActiveRegion());

    // 注意：当玩家有 regionBuilder 时，会自动生成 region，且理论上 regionBuilder 和 region 进行的操作应当是一致的。
    ((ServerPlayerEntityExtension) player).ec$setActiveRegion(operatedRegion);
    CommandBridge.sendFeedback(source, () -> messageSingle.apply(player, operatedRegion), true);
    return 1;
  }
}
