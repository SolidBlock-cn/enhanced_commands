package pers.solid.ecmd.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.DirectionArgumentType;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.lambda.ToFloatTriFunction;
import pers.solid.ecmd.util.lambda.ToIntQuadFunction;
import pers.solid.ecmd.util.lambda.ToIntTriFunction;
import pers.solid.ecmd.util.lambda.TriPredicate;

public enum TestForBlockInfoCommand implements TestForCommands.Entry {
  INSTANCE;

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockInfoCommandProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder) {
    return argumentBuilder
        .then(CommandManager.argument("pos", EnhancedPosArgumentType.blockPos())
            .then(CommandManager.literal("hardness")
                .executes(context -> executeGetHardness(context, 1))
                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                    .executes(context -> executeGetHardness(context, FloatArgumentType.getFloat(context, "scale")))))
            .then(CommandManager.literal("luminance")
                .executes(TestForBlockInfoCommand::executeGetLuminance))
            .then(CommandManager.literal("strong_redstone_power")
                .then(CommandManager.argument("direction", DirectionArgumentType.direction())
                    .executes(TestForBlockInfoCommand::executeGetStrongRedstonePower)))
            .then(CommandManager.literal("weak_redstone_power")
                .then(CommandManager.argument("direction", DirectionArgumentType.direction())
                    .executes(TestForBlockInfoCommand::executeGetWeakRedstonePower)))
            .then(CommandManager.literal("light")
                .executes(context -> executeGetLight(context, null)))
            .then(CommandManager.literal("block_light")
                .executes(context -> executeGetLight(context, LightType.BLOCK)))
            .then(CommandManager.literal("sky_light")
                .executes(context -> executeGetLight(context, LightType.SKY)))
            .then(CommandManager.literal("emits_redstone_power")
                .executes(TestForBlockInfoCommand::executeGetEmitsRedstonePower))
            .then(CommandManager.literal("opaque")
                .executes(TestForBlockInfoCommand::executeGetOpaque))
            .then(CommandManager.literal("model_offset")
                .executes(TestForBlockInfoCommand::executeGetModelOffset))
            .then(CommandManager.literal("suffocate")
                .executes(TestForBlockInfoCommand::executeGetSuffocate))
            .then(CommandManager.literal("block_vision")
                .executes(TestForBlockInfoCommand::executeGetBlockVision))
            .then(CommandManager.literal("replaceable")
                .executes(TestForBlockInfoCommand::executeGetReplaceable))
            .then(CommandManager.literal("random_ticks")
                .executes(TestForBlockInfoCommand::executeGetRandomTicks)));
  }

  private static int getIntBlockInfo(CommandContext<ServerCommandSource> context, String translationKey, ToIntTriFunction<BlockState, ServerWorld, BlockPos> function) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final int value = function.applyAsInt(blockState, world, pos);
    CommandBridge.sendFeedback(source, () -> Text.translatable(translationKey, blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos), Text.literal(String.valueOf(value)).styled(TextUtil.STYLE_FOR_ACTUAL)), false);
    return value;
  }

  private static int getIntBlockInfoWithDirection(CommandContext<ServerCommandSource> context, String translationKey, ToIntQuadFunction<BlockState, ServerWorld, BlockPos, Direction> function) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final Direction direction = DirectionArgumentType.getDirection(context, "direction");
    final int value = function.applyAsInt(blockState, world, pos, direction);
    CommandBridge.sendFeedback(source, () -> Text.translatable(translationKey, blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos), Text.literal(String.valueOf(value)).styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.wrapDirection(direction).styled(TextUtil.STYLE_FOR_TARGET)), false);
    return value;
  }

  private static float getFloatBlockInfo(CommandContext<ServerCommandSource> context, String translationKey, ToFloatTriFunction<BlockState, ServerWorld, BlockPos> function) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final float value = function.applyAsFloat(blockState, world, pos);
    CommandBridge.sendFeedback(source, () -> Text.translatable(translationKey, blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos), Text.literal(String.valueOf(value)).styled(TextUtil.STYLE_FOR_ACTUAL)), false);
    return value;
  }

  private static boolean getBooleanBlockInfo(CommandContext<ServerCommandSource> context, String translationKeyWhenFalse, String translationKeyWhenTrue, TriPredicate<BlockState, ServerWorld, BlockPos> predicate) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final boolean value = predicate.test(blockState, world, pos);
    CommandBridge.sendFeedback(source, () -> Text.translatable(value ? translationKeyWhenTrue : translationKeyWhenFalse, blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos)), false);
    return value;
  }

  private static int executeGetHardness(CommandContext<ServerCommandSource> context, float scale) throws CommandSyntaxException {
    final double hardness = getFloatBlockInfo(context, "enhanced_commands.commands.testfor.block_info.hardness", AbstractBlock.AbstractBlockState::getHardness);
    return (int) (hardness * scale);
  }

  private static int executeGetLuminance(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return getIntBlockInfo(context, "enhanced_commands.commands.testfor.block_info.luminance", (blockState, serverWorld, blockPos) -> blockState.getLuminance());
  }

  private static int executeGetStrongRedstonePower(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return getIntBlockInfoWithDirection(context, "enhanced_commands.commands.testfor.block_info.strong_redstone_power", AbstractBlock.AbstractBlockState::getStrongRedstonePower);
  }

  private static int executeGetWeakRedstonePower(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return getIntBlockInfoWithDirection(context, "enhanced_commands.commands.testfor.block_info.weak_redstone_power", AbstractBlock.AbstractBlockState::getWeakRedstonePower);
  }

  private static int executeGetLight(CommandContext<ServerCommandSource> context, @Nullable LightType lightType) throws CommandSyntaxException {
    if (lightType == null) {
      return getIntBlockInfo(context, "enhanced_commands.commands.testfor.block_info.light", (blockState, serverWorld, blockPos) -> serverWorld.getLightLevel(blockPos));
    } else {
      return getIntBlockInfo(context, "enhanced_commands.commands.testfor.block_info." + (lightType == LightType.BLOCK ? "block" : "sky") + "_light", (blockState, serverWorld, blockPos) -> serverWorld.getLightLevel(lightType, blockPos));
    }
  }

  private static int executeGetEmitsRedstonePower(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.block_info.emits_redstone_power.false", "enhanced_commands.commands.testfor.block_info.emits_redstone_power.true", (blockState, serverWorld, blockPos) -> blockState.emitsRedstonePower()));
  }

  private static int executeGetOpaque(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.block_info.opaque.false", "enhanced_commands.commands.testfor.block_info.opaque.true", (blockState, serverWorld, blockPos) -> blockState.isOpaque()));
  }

  private static int executeGetModelOffset(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final Vec3d modelOffset = blockState.getModelOffset(world, pos);
    if (modelOffset.equals(Vec3d.ZERO)) {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.testfor.block_info.model_offset.false", blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos)), false);
      return 0;
    } else {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.testfor.block_info.model_offset.true", blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos), TextUtil.wrapVector(modelOffset)), false);
      return 1;
    }
  }

  private static int executeGetSuffocate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.block_info.suffocate.false", "enhanced_commands.commands.testfor.block_info.suffocate.true", AbstractBlock.AbstractBlockState::shouldSuffocate));
  }

  private static int executeGetBlockVision(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.block_info.block_vision.false", "enhanced_commands.commands.testfor.block_info.block_vision.true", AbstractBlock.AbstractBlockState::shouldBlockVision));
  }

  private static int executeGetReplaceable(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.block_info.replaceable.false", "enhanced_commands.commands.testfor.block_info.replaceable.true", (blockState, serverWorld, blockPos) -> blockState.isReplaceable()));
  }

  private static int executeGetRandomTicks(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhanced_commands.commands.testfor.block_info.random_ticks.false", "enhanced_commands.commands.testfor.block_info.random_ticks.true", (blockState, serverWorld, blockPos) -> blockState.hasRandomTicks()));
  }

  @Override
  public void addArguments(LiteralArgumentBuilder<ServerCommandSource> testForBuilder, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    testForBuilder.then(addBlockInfoCommandProperties(CommandManager.literal("block_info")));
  }
}
