package pers.solid.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;
import pers.solid.mod.EnhancedCommands;
import pers.solid.mod.argument.BlockPredicateArgumentType;
import pers.solid.mod.argument.DirectionArgumentType;
import pers.solid.mod.util.ToFloatTriFunction;
import pers.solid.mod.util.ToIntQuadFunction;
import pers.solid.mod.util.ToIntTriFunction;
import pers.solid.mod.util.TriPredicate;

import java.util.Collection;

public final class TestForCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
    dispatcher.register(CommandManager.literal("testfor")
        .requires(ModCommands.REQUIRES_PERMISSION_2)
        .then(addBlockCommandProperties(CommandManager.literal("block"), registryAccess))
        .then(addBlockInfoCommandProperties(CommandManager.literal("block_info"), registryAccess)));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockCommandProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder
        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
            .executes(TestForCommand::executesTestForBlock)
            .then(CommandManager.argument("predicate", new BlockPredicateArgumentType(registryAccess))
                .executes(context -> {
                  final ServerCommandSource source = context.getSource();
                  final TestResult testResult = BlockPredicateArgumentType.getBlockPredicate(context, "predicate").testAndDescribe(new CachedBlockPosition(source.getWorld(), BlockPosArgumentType.getBlockPos(context, "pos"), false));
                  testResult.sendMessage(source);
                  return BooleanUtils.toInteger(testResult.successes());
                })));
  }

  private static int executesTestForBlock(CommandContext<ServerCommandSource> context) {
    final BlockPos blockPos = BlockPosArgumentType.getBlockPos(context, "pos");
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(blockPos);
    final Collection<Property<?>> properties = blockState.getProperties();
    source.sendFeedback(Text.translatable(properties.isEmpty() ? "enhancedCommands.testfor.block.info" : "enhancedCommands.testfor.block.info_with_properties", EnhancedCommands.wrapBlockPos(blockPos), blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_ACTUAL), Text.literal(Registries.BLOCK.getId(blockState.getBlock()).toString()).styled(EnhancedCommands.STYLE_FOR_ACTUAL)), true);
    for (Property<?> property : properties) {
      source.sendFeedback(expressPropertyValue(blockState, property), true);
    }
    return 1;
  }

  private static <T extends Comparable<T>> MutableText expressPropertyValue(BlockState blockState, Property<T> property) {
    final MutableText text = Text.literal("  ").append(property.getName()).append(" = ");
    final T value = blockState.get(property);
    return text.append(value instanceof Boolean bool ? Text.literal(property.name(value)).formatted(bool ? Formatting.GREEN : Formatting.RED) : Text.literal(property.name(value)));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockInfoCommandProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder
        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
            .then(CommandManager.literal("hardness")
                .executes(context -> executeGetHardness(context, 1))
                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                    .executes(context -> executeGetHardness(context, FloatArgumentType.getFloat(context, "scale")))))
            .then(CommandManager.literal("luminance")
                .executes(TestForCommand::executeGetLuminance))
            .then(CommandManager.literal("strong_redstone_power")
                .then(CommandManager.argument("direction", DirectionArgumentType.create())
                    .executes(TestForCommand::executeGetStrongRedstonePower)))
            .then(CommandManager.literal("weak_redstone_power")
                .then(CommandManager.argument("direction", DirectionArgumentType.create())
                    .executes(TestForCommand::executeGetWeakRedstonePower)))
            .then(CommandManager.literal("light")
                .executes(context -> executeGetLight(context, null)))
            .then(CommandManager.literal("block_light")
                .executes(context -> executeGetLight(context, LightType.BLOCK)))
            .then(CommandManager.literal("sky_light")
                .executes(context -> executeGetLight(context, LightType.SKY)))
            .then(CommandManager.literal("emits_redstone_power")
                .executes(TestForCommand::executeGetEmitsRedstonePower))
            .then(CommandManager.literal("opaque")
                .executes(TestForCommand::executeGetOpaque))
            .then(CommandManager.literal("model_offset")
                .executes(TestForCommand::executeGetModelOffset))
            .then(CommandManager.literal("suffocate")
                .executes(TestForCommand::executeGetSuffocate))
            .then(CommandManager.literal("block_vision")
                .executes(TestForCommand::executeGetBlockVision))
            .then(CommandManager.literal("replaceable")
                .executes(TestForCommand::executeGetReplaceable))
            .then(CommandManager.literal("random_ticks")
                .executes(TestForCommand::executeGetRandomTicks)));
  }

  private static int getIntBlockInfo(CommandContext<ServerCommandSource> context, String translationKey, ToIntTriFunction<BlockState, ServerWorld, BlockPos> function) {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final int value = function.applyAsInt(blockState, world, pos);
    source.sendFeedback(Text.translatable(translationKey, blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_TARGET), EnhancedCommands.wrapBlockPos(pos), Text.literal(String.valueOf(value)).styled(EnhancedCommands.STYLE_FOR_ACTUAL)), true);
    return value;
  }

  private static int getIntBlockInfoWithDirection(CommandContext<ServerCommandSource> context, String translationKey, ToIntQuadFunction<BlockState, ServerWorld, BlockPos, Direction> function) {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final Direction direction = DirectionArgumentType.getDirection(context, "direction");
    final int value = function.applyAsInt(blockState, world, pos, direction);
    source.sendFeedback(Text.translatable(translationKey, blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_TARGET), EnhancedCommands.wrapBlockPos(pos), Text.literal(String.valueOf(value)).styled(EnhancedCommands.STYLE_FOR_ACTUAL), EnhancedCommands.wrapDirection(direction).styled(EnhancedCommands.STYLE_FOR_TARGET)), true);
    return value;
  }

  private static float getFloatBlockInfo(CommandContext<ServerCommandSource> context, String translationKey, ToFloatTriFunction<BlockState, ServerWorld, BlockPos> function) {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final float value = function.applyAsFloat(blockState, world, pos);
    source.sendFeedback(Text.translatable(translationKey, blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_TARGET), EnhancedCommands.wrapBlockPos(pos), Text.literal(String.valueOf(value)).styled(EnhancedCommands.STYLE_FOR_ACTUAL)), true);
    return value;
  }

  private static boolean getBooleanBlockInfo(CommandContext<ServerCommandSource> context, String translationKeyWhenFalse, String translationKeyWhenTrue, TriPredicate<BlockState, ServerWorld, BlockPos> predicate) {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final boolean value = predicate.test(blockState, world, pos);
    source.sendFeedback(Text.translatable(value ? translationKeyWhenTrue : translationKeyWhenFalse, blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_TARGET), EnhancedCommands.wrapBlockPos(pos)), true);
    return value;
  }

  private static int executeGetHardness(CommandContext<ServerCommandSource> context, float scale) {
    final double hardness = getFloatBlockInfo(context, "enhancedCommands.testfor.block_info.hardness", AbstractBlock.AbstractBlockState::getHardness);
    return (int) (hardness * scale);
  }

  private static int executeGetLuminance(CommandContext<ServerCommandSource> context) {
    return getIntBlockInfo(context, "enhancedCommands.testfor.block_info.luminance", (blockState, serverWorld, blockPos) -> blockState.getLuminance());
  }

  private static int executeGetStrongRedstonePower(CommandContext<ServerCommandSource> context) {
    return getIntBlockInfoWithDirection(context, "enhancedCommands.testfor.block_info.strong_redstone_power", AbstractBlock.AbstractBlockState::getStrongRedstonePower);
  }

  private static int executeGetWeakRedstonePower(CommandContext<ServerCommandSource> context) {
    return getIntBlockInfoWithDirection(context, "enhancedCommands.testfor.block_info.weak_redstone_power", AbstractBlock.AbstractBlockState::getWeakRedstonePower);
  }

  private static int executeGetLight(CommandContext<ServerCommandSource> context, @Nullable LightType lightType) {
    if (lightType == null) {
      return getIntBlockInfo(context, "enhancedCommands.testfor.block_info.light", (blockState, serverWorld, blockPos) -> serverWorld.getLightLevel(blockPos));
    } else {
      return getIntBlockInfo(context, "enhancedCommands.testfor.block_info." + (lightType == LightType.BLOCK ? "block" : "sky") + "_light", (blockState, serverWorld, blockPos) -> serverWorld.getLightLevel(lightType, blockPos));
    }
  }

  private static int executeGetEmitsRedstonePower(CommandContext<ServerCommandSource> context) {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhancedCommands.testfor.block_info.emits_redstone_power.false", "enhancedCommands.testfor.block_info.emits_redstone_power.true", (blockState, serverWorld, blockPos) -> blockState.emitsRedstonePower()));
  }

  private static int executeGetOpaque(CommandContext<ServerCommandSource> context) {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhancedCommands.testfor.block_info.opaque.false", "enhancedCommands.testfor.block_info.opaque.true", (blockState, serverWorld, blockPos) -> blockState.isOpaque()));
  }

  private static int executeGetModelOffset(CommandContext<ServerCommandSource> context) {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final Vec3d modelOffset = blockState.getModelOffset(world, pos);
    if (modelOffset.equals(Vec3d.ZERO)) {
      source.sendFeedback(Text.translatable("enhancedCommands.testfor.block_info.model_offset.false", blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_TARGET), EnhancedCommands.wrapBlockPos(pos)), true);
      return 0;
    } else {
      source.sendFeedback(Text.translatable("enhancedCommands.testfor.block_info.model_offset.true", blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_TARGET), EnhancedCommands.wrapBlockPos(pos), EnhancedCommands.wrapPosition(modelOffset)), true);
      return 1;
    }
  }

  private static int executeGetSuffocate(CommandContext<ServerCommandSource> context) {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhancedCommands.testfor.block_info.suffocate.false", "enhancedCommands.testfor.block_info.suffocate.true", AbstractBlock.AbstractBlockState::shouldSuffocate));
  }

  private static int executeGetBlockVision(CommandContext<ServerCommandSource> context) {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhancedCommands.testfor.block_info.block_vision.false", "enhancedCommands.testfor.block_info.block_vision.true", AbstractBlock.AbstractBlockState::shouldBlockVision));
  }

  private static int executeGetReplaceable(CommandContext<ServerCommandSource> context) {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhancedCommands.testfor.block_info.replaceable.false", "enhancedCommands.testfor.block_info.replaceable.true", (blockState, serverWorld, blockPos) -> blockState.isReplaceable()));
  }

  private static int executeGetRandomTicks(CommandContext<ServerCommandSource> context) {
    return BooleanUtils.toInteger(getBooleanBlockInfo(context, "enhancedCommands.testfor.block_info.random_ticks.false", "enhancedCommands.testfor.block_info.random_ticks.true", (blockState, serverWorld, blockPos) -> blockState.hasRandomTicks()));
  }
}
