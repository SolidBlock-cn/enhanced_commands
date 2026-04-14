package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.AxisProvider;
import pers.solid.ecmd.argument.DirectionProvider;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.enums.CommandEnumType;

import java.util.function.BiFunction;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.argument.DirectionArgument.direction;
import static pers.solid.ecmd.argument.DirectionArgument.getDirection;
import static pers.solid.ecmd.argument.SimpleEnumArgument.*;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum ActiveRegionCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  public static final SimpleCommandExceptionType UNSUPPORTED = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.commands.activeregion.unsupported"));
  public static final DynamicCommandExceptionType UNSUPPORTED_WITH_REASON = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.activeregion.unsupported_with_region", o));

  public static int executeGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final ServerPlayer player = context.getSource().getPlayerOrException();
    final @Nullable Region region;
    try {
      region = player.getActiveRegionOrThrow$ec().region();
    } catch (CommandRuntimeException e) {
      throw new CommandSyntaxException(null, e.rawMessage);
    }
    if (region == null) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.activeregion.get_none", TextUtil.styled(player.getDisplayName(), Styles.TARGET)), false);
      return 0;
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.activeregion.get", TextUtil.styled(player.getDisplayName(), Styles.TARGET), TextUtil.literal(region).withStyle(Styles.RESULT)), false);
      return 1;
    }
  }

  public static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final Player player = source.getPlayerOrException();
    source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.activeregion.set.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET)), true);
    return 1;
  }

  public static int executeRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final Player player = source.getPlayerOrException();
    player.setActiveRegion$ec(null);
    source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.activeregion.remove.single", TextUtil.styled(player.getDisplayName(), Styles.TARGET)), true);
    return 1;
  }

  public static <T, R> R invokeOperationOrThrow(FailableFunction<T, R, CommandSyntaxException> function, T input) throws CommandSyntaxException {
    try {
      return function.apply(input);
    } catch (Throwable e) {
      if (e.getCause() instanceof CommandSyntaxException c) {
        throw UNSUPPORTED_WITH_REASON.create(c.getRawMessage());
      } else if (e instanceof CommandSyntaxException) {
        throw e;
      } else {
        throw UNSUPPORTED.create();
      }
    }
  }

  public static int executeMoveDirection(double offset, Direction direction, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(Vec3.atLowerCornerOf(direction.getNormal()).scale(offset)), (serverPlayerEntity, region) -> Component.translatable("enhanced_commands.commands.activeregion.move.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), Component.literal(region.asString()).withStyle(Styles.RESULT)), context);
  }

  public static int executeMoveVector(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeMoveVector(getDouble(context, "x"), getDouble(context, "y"), getDouble(context, "z"), context);
  }

  public static int executeMoveVector(double x, double y, double z, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeMoveVector(new Vec3(x, y, z), context);
  }

  public static int executeMoveVector(Vec3 vec3d, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.moved(vec3d), (serverPlayerEntity, region) -> Component.translatable("enhanced_commands.commands.activeregion.move.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(region.region()).withStyle(Styles.RESULT)), context);
  }

  public static int executeRotate(Vec3 pivot, Rotation blockRotation, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.rotated(blockRotation, pivot), (serverPlayerEntity, region) -> Component.translatable("enhanced_commands.commands.activeregion.rotate.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(region.region()).withStyle(Styles.RESULT)), context);
  }

  public static int executeMirror(Vec3 pivot, Direction.Axis axis, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.mirrored(axis, pivot), (serverPlayerEntity, region) -> Component.translatable("enhanced_commands.commands.activeregion.mirror.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(region.region()).withStyle(Styles.RESULT)), context);
  }

  public static int executeExpandDirection(double offset, Direction direction, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset, direction), (serverPlayerEntity, region) -> Component.translatable("enhanced_commands.commands.activeregion.expand.direction.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(offset), TextUtil.wrapDirection(direction), TextUtil.literal(region.region()).withStyle(Styles.RESULT)), context);
  }

  public static int executeExpandAxis(double offset, Direction.Axis axis, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset, axis), (serverPlayerEntity, region) -> Component.translatable("enhanced_commands.commands.activeregion.expand.axis.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(offset), TextUtil.wrapAxis(axis), TextUtil.literal(region.region()).withStyle(Styles.RESULT)), context);
  }

  public static int executeExpandDirectionType(double offset, Direction.Plane type, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final MutableComponent adverb = Component.translatable("enhanced_commands.direction_type." + type.name().toLowerCase() + ".adverb");
    return executeRegionModification(region -> region.expanded(offset, type), (serverPlayerEntity, region) -> Component.translatable("enhanced_commands.commands.activeregion.expand.axis.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(offset), adverb, TextUtil.literal(region.region()).withStyle(Styles.RESULT)), context);
  }

  public static int executeExpandAllDirections(double offset, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeRegionModification(region -> region.expanded(offset), (serverPlayerEntity, region) -> Component.translatable("enhanced_commands.commands.activeregion.expand.all_directions.single", TextUtil.styled(serverPlayerEntity.getName(), Styles.TARGET), TextUtil.literal(offset), TextUtil.literal(region.region()).withStyle(Styles.RESULT)), context);
  }

  public static int executeRegionModification(FailableFunction<RegionSelection, RegionSelection, CommandSyntaxException> regionOperation, BiFunction<ServerPlayer, RegionSelection, Component> messageSingle, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final ServerPlayer player = source.getPlayerOrException();
    final RegionSelection operatedRegion = invokeOperationOrThrow(regionOperation, player.getActiveRegion$ec());

    // 注意：当玩家有 regionBuilder 时，会自动生成 region，且理论上 regionBuilder 和 region 进行的操作应当是一致的。
    player.setActiveRegion$ec(operatedRegion);
    source.sendFeedback$ecBridge(() -> messageSingle.apply(player, operatedRegion), true);
    return 1;
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final LiteralCommandNode<CommandSourceStack> literalCommandNode = dispatcher.register(literalR2("activeregion")
        .then(literal("get")
            .executes(ActiveRegionCommand::executeGet))
        .then(literal("remove")
            .executes(ActiveRegionCommand::executeRemove))
        .then(literal("move")
            .executes(context -> executeMoveDirection(1, DirectionProvider.FRONT.apply(context.getSource()), context))
            .then(argument("amount", doubleArg())
                .executes(context -> executeMoveDirection(getDouble(context, "amount"), DirectionProvider.FRONT.apply(context.getSource()), context))
                .then(argument("direction", direction())
                    .executes(context -> executeMoveDirection(getDouble(context, "amount"), getDirection(context, "direction"), context))))
            .then(argument("x", doubleArg())
                .then(argument("y", doubleArg())
                    .then(argument("z", doubleArg())
                        .executes(ActiveRegionCommand::executeMoveVector)))))
        .then(literal("rotate")
            .then(argument("rotation", TemplateRotationArgument.templateRotation())
                .executes(context -> executeRotate(EnhancedPosArgument.CURRENT_BLOCK_POS_CENTER.getPosition(context.getSource()), TemplateRotationArgument.getRotation(context, "rotation"), context))
                .then(argument("pivot", EnhancedPosArgument.posPreferringCenteredInt())
                    .executes(context -> executeRotate(EnhancedPosArgument.getPos(context, "pivot"), TemplateRotationArgument.getRotation(context, "rotation"), context)))))
        .then(literal("mirror")
            .executes(context -> executeMirror(EnhancedPosArgument.CURRENT_BLOCK_POS_CENTER.getPosition(context.getSource()), AxisProvider.FRONT_BACK.apply(context.getSource()), context))
            .then(argument("axis", axis(false))
                .executes(context -> executeMirror(EnhancedPosArgument.CURRENT_BLOCK_POS_CENTER.getPosition(context.getSource()), getAxis(context, "axis"), context))
                .then(argument("pivot", EnhancedPosArgument.posPreferringCenteredInt())
                    .executes(context -> executeMirror(EnhancedPosArgument.getPos(context, "pivot"), getAxis(context, "axis"), context)))))
        .then(literal("expand")
            .executes(context -> executeExpandDirection(1, DirectionProvider.FRONT.apply(context.getSource()), context))
            .then(argument("offset", doubleArg())
                .executes(context -> executeExpandDirection(getDouble(context, "offset"), DirectionProvider.FRONT.apply(context.getSource()), context))
                .then(argument("direction", direction())
                    .executes(context -> executeExpandDirection(getDouble(context, "offset"), getDirection(context, "direction"), context)))
                .then(argument("axis", axis(true))
                    .executes(context -> executeExpandAxis(getDouble(context, "offset"), getAxis(context, "axis"), context)))
                .then(argument("direction_type", simpleEnum(CommandEnumType.DIRECTION_TYPE))
                    .executes(context -> executeExpandDirectionType(getDouble(context, "offset"), context.getArgument("direction_type", Direction.Plane.class), context)))
                .then(literal("all")
                    .executes(context -> executeExpandAllDirections(getDouble(context, "offset"), context))))
            .then(argument("direction", direction())
                .executes(context -> executeExpandDirection(1, getDirection(context, "direction"), context)))
            .then(argument("axis", axis(true))
                .executes(context -> executeExpandAxis(1, getAxis(context, "axis"), context)))
            .then(argument("direction_type", simpleEnum(CommandEnumType.DIRECTION_TYPE))
                .executes(context -> executeExpandDirectionType(1, context.getArgument("direction_type", Direction.Plane.class), context)))
            .then(literal("all")
                .executes(context -> executeExpandAllDirections(1, context)))));
    dispatcher.register(literalR2("ar").redirect(literalCommandNode));
  }
}
