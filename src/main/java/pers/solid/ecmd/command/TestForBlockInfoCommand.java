package pers.solid.ecmd.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.DirectionArgument;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.lambda.ToFloatTriFunction;
import pers.solid.ecmd.util.lambda.ToIntQuadFunction;
import pers.solid.ecmd.util.lambda.ToIntTriFunction;
import pers.solid.ecmd.util.lambda.TriPredicate;

public enum TestForBlockInfoCommand implements TestForCommands.Entry {
  INSTANCE;

  private static LiteralArgumentBuilder<CommandSourceStack> addBlockInfoCommandProperties(LiteralArgumentBuilder<CommandSourceStack> argumentBuilder) {
    return argumentBuilder
        .then(Commands.argument("pos", EnhancedPosArgument.blockPos())
            .then(Commands.literal("hardness")
                .executes(context -> executeGetHardness(context, 1))
                .then(Commands.argument("scale", FloatArgumentType.floatArg())
                    .executes(context -> executeGetHardness(context, FloatArgumentType.getFloat(context, "scale")))))
            .then(Commands.literal("luminance")
                .executes(TestForBlockInfoCommand::executeGetLuminance))
            .then(Commands.literal("strong_redstone_power")
                .then(Commands.argument("direction", DirectionArgument.direction())
                    .executes(TestForBlockInfoCommand::executeGetStrongRedstonePower)))
            .then(Commands.literal("weak_redstone_power")
                .then(Commands.argument("direction", DirectionArgument.direction())
                    .executes(TestForBlockInfoCommand::executeGetWeakRedstonePower)))
            .then(Commands.literal("light")
                .executes(context -> executeGetLight(context, null)))
            .then(Commands.literal("block_light")
                .executes(context -> executeGetLight(context, LightLayer.BLOCK)))
            .then(Commands.literal("sky_light")
                .executes(context -> executeGetLight(context, LightLayer.SKY)))
            .then(Commands.literal("emits_redstone_power")
                .executes(TestForBlockInfoCommand::executeGetEmitsRedstonePower))
            .then(Commands.literal("opaque")
                .executes(TestForBlockInfoCommand::executeGetOpaque))
            .then(Commands.literal("model_offset")
                .executes(TestForBlockInfoCommand::executeGetModelOffset))
            .then(Commands.literal("suffocate")
                .executes(TestForBlockInfoCommand::executeGetSuffocate))
            .then(Commands.literal("block_vision")
                .executes(TestForBlockInfoCommand::executeGetBlockVision))
            .then(Commands.literal("replaceable")
                .executes(TestForBlockInfoCommand::executeGetReplaceable))
            .then(Commands.literal("random_ticks")
                .executes(TestForBlockInfoCommand::executeGetRandomTicks)));
  }

  private static int getIntBlockInfo(CommandContext<CommandSourceStack> context, String translationKey, ToIntTriFunction<BlockState, ServerLevel, BlockPos> function) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
    final ServerLevel world = source.getLevel();
    final BlockState blockState = world.getBlockState(pos);
    final int value = function.applyAsInt(blockState, world, pos);
    source.sendFeedback$ecBridge(() -> Component.translatable(translationKey, blockState.getBlock().getName().withStyle(Styles.TARGET), TextUtil.wrapVector(pos), Component.literal(String.valueOf(value)).withStyle(Styles.ACTUAL)), false);
    return value;
  }

  private static int getIntBlockInfoWithDirection(CommandContext<CommandSourceStack> context, String translationKey, ToIntQuadFunction<BlockState, ServerLevel, BlockPos, Direction> function) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
    final ServerLevel world = source.getLevel();
    final BlockState blockState = world.getBlockState(pos);
    final Direction direction = DirectionArgument.getDirection(context, "direction");
    final int value = function.applyAsInt(blockState, world, pos, direction);
    source.sendFeedback$ecBridge(() -> Component.translatable(translationKey, blockState.getBlock().getName().withStyle(Styles.TARGET), TextUtil.wrapVector(pos), Component.literal(String.valueOf(value)).withStyle(Styles.ACTUAL), TextUtil.wrapDirection(direction).withStyle(Styles.TARGET)), false);
    return value;
  }

  private static float getFloatBlockInfo(CommandContext<CommandSourceStack> context, String translationKey, ToFloatTriFunction<BlockState, ServerLevel, BlockPos> function) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
    final ServerLevel world = source.getLevel();
    final BlockState blockState = world.getBlockState(pos);
    final float value = function.applyAsFloat(blockState, world, pos);
    source.sendFeedback$ecBridge(() -> Component.translatable(translationKey, blockState.getBlock().getName().withStyle(Styles.TARGET), TextUtil.wrapVector(pos), Component.literal(String.valueOf(value)).withStyle(Styles.ACTUAL)), false);
    return value;
  }

  private static boolean getBooleanBlockInfo(CommandContext<CommandSourceStack> context, String translationKeyWhenFalse, String translationKeyWhenTrue, TriPredicate<BlockState, ServerLevel, BlockPos> predicate) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
    final ServerLevel world = source.getLevel();
    final BlockState blockState = world.getBlockState(pos);
    final boolean value = predicate.test(blockState, world, pos);
    source.sendFeedback$ecBridge(() -> Component.translatable(value ? translationKeyWhenTrue : translationKeyWhenFalse, blockState.getBlock().getName().withStyle(Styles.TARGET), TextUtil.wrapVector(pos)), false);
    return value;
  }

  private static int executeGetHardness(CommandContext<CommandSourceStack> context, float scale) throws CommandSyntaxException {
    final double hardness = getFloatBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.hardness", BlockBehaviour.BlockStateBase::getDestroySpeed);
    return (int) (hardness * scale);
  }

  private static int executeGetLuminance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return getIntBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.luminance", (blockState, serverWorld, blockPos) -> blockState.getLightEmission());
  }

  private static int executeGetStrongRedstonePower(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return getIntBlockInfoWithDirection(context, "enhanced_commands.commands.testfor.blockinfo.strong_redstone_power", BlockBehaviour.BlockStateBase::getDirectSignal);
  }

  private static int executeGetWeakRedstonePower(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return getIntBlockInfoWithDirection(context, "enhanced_commands.commands.testfor.blockinfo.weak_redstone_power", BlockBehaviour.BlockStateBase::getSignal);
  }

  private static int executeGetLight(CommandContext<CommandSourceStack> context, @Nullable LightLayer lightType) throws CommandSyntaxException {
    if (lightType == null) {
      return getIntBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.light", (blockState, serverWorld, blockPos) -> serverWorld.getMaxLocalRawBrightness(blockPos));
    } else {
      return getIntBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo." + (lightType == LightLayer.BLOCK ? "block" : "sky") + "_light", (blockState, serverWorld, blockPos) -> serverWorld.getBrightness(lightType, blockPos));
    }
  }

  private static int executeGetEmitsRedstonePower(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.emits_redstone_power.false", "enhanced_commands.commands.testfor.blockinfo.emits_redstone_power.true", (blockState, serverWorld, blockPos) -> blockState.isSignalSource()));
  }

  private static int executeGetOpaque(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.opaque.false", "enhanced_commands.commands.testfor.blockinfo.opaque.true", (blockState, serverWorld, blockPos) -> blockState.canOcclude()));
  }

  private static int executeGetModelOffset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final BlockPos pos = EnhancedPosArgument.getLoadedBlockPos(context, "pos");
    final ServerLevel world = source.getLevel();
    final BlockState blockState = world.getBlockState(pos);
    final Vec3 modelOffset = blockState.getOffset(pos);
    if (modelOffset.equals(Vec3.ZERO)) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blockinfo.model_offset.false", blockState.getBlock().getName().withStyle(Styles.TARGET), TextUtil.wrapVector(pos)), false);
      return 0;
    } else {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blockinfo.model_offset.true", blockState.getBlock().getName().withStyle(Styles.TARGET), TextUtil.wrapVector(pos), TextUtil.wrapVector(modelOffset)), false);
      return 1;
    }
  }

  private static int executeGetSuffocate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.suffocate.false", "enhanced_commands.commands.testfor.blockinfo.suffocate.true", BlockBehaviour.BlockStateBase::isSuffocating));
  }

  private static int executeGetBlockVision(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.block_vision.false", "enhanced_commands.commands.testfor.blockinfo.block_vision.true", BlockBehaviour.BlockStateBase::isViewBlocking));
  }

  private static int executeGetReplaceable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.replaceable.false", "enhanced_commands.commands.testfor.blockinfo.replaceable.true", (blockState, serverWorld, blockPos) -> blockState.canBeReplaced()));
  }

  private static int executeGetRandomTicks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.blockinfo.random_ticks.false", "enhanced_commands.commands.testfor.blockinfo.random_ticks.true", (blockState, serverWorld, blockPos) -> blockState.isRandomlyTicking()));
  }

  @Override
  public void addArguments(LiteralArgumentBuilder<CommandSourceStack> testForBuilder, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    testForBuilder.then(addBlockInfoCommandProperties(Commands.literal("blockinfo")));
  }
}
