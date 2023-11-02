package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
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
import pers.solid.ecmd.argument.BlockPredicateArgumentType;
import pers.solid.ecmd.argument.DirectionArgumentType;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.lambda.ToFloatTriFunction;
import pers.solid.ecmd.util.lambda.ToIntQuadFunction;
import pers.solid.ecmd.util.lambda.ToIntTriFunction;
import pers.solid.ecmd.util.lambda.TriPredicate;

import java.util.Collection;

public enum TestForCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final KeywordArgsArgumentType BLOCK_KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("force_load", BoolArgumentType.bool(), false)
      .build();
  public static final DynamicCommandExceptionType TEST_FOR_BLOCK_NOT_LOADED = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.testfor.block.not_loaded", o));
  public static final DynamicCommandExceptionType TEST_FOR_BLOCK_PREDICATE_NOT_LOADED = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.testfor.block.not_loaded_for_predicate", o));

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockCommandProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder
        .then(CommandManager.argument("pos", EnhancedPosArgumentType.blockPos())
            .executes(TestForCommand::executeTestForBlock)
            .then(CommandManager.argument("predicate", new BlockPredicateArgumentType(registryAccess))
                .executes(context -> executeTestForBlockPredicate(context, false))
                .then(CommandManager.argument("keyword_args", BLOCK_KEYWORD_ARGS)
                    .executes(context -> executeTestForBlockPredicate(context, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args").getBoolean("force_load"))))));
  }

  private static int executeTestForBlock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final BlockPos blockPos = EnhancedPosArgumentType.getBlockPos(context, "pos");
    // 会检查区块已加载，不过不是在这里，而是在下面。
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    if (!world.isChunkLoaded(blockPos)) {
      throw TEST_FOR_BLOCK_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final BlockState blockState = world.getBlockState(blockPos);
    final Collection<Property<?>> properties = blockState.getProperties();
    CommandBridge.sendFeedback(source, () -> Text.translatable(properties.isEmpty() ? "enhanced_commands.commands.testfor.block.info" : "enhanced_commands.commands.testfor.block.info_with_properties", TextUtil.wrapVector(blockPos), blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(Registries.BLOCK.getId(blockState.getBlock())).styled(TextUtil.STYLE_FOR_ACTUAL)), true);
    for (Property<?> property : properties) {
      CommandBridge.sendFeedback(source, () -> expressPropertyValue(blockState, property), true);
    }
    return 1;
  }

  private static int executeTestForBlockPredicate(CommandContext<ServerCommandSource> context, boolean forceLoad) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos blockPos = EnhancedPosArgumentType.getBlockPos(context, "pos");
    // 检查方块的代码在后面
    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(source.getWorld(), blockPos, forceLoad);
    if (cachedBlockPosition.getBlockState() == null) {
      throw TEST_FOR_BLOCK_PREDICATE_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final TestResult testResult = BlockPredicateArgumentType.getBlockPredicate(context, "predicate").testAndDescribe(cachedBlockPosition);
    testResult.sendMessage(source);
    return BooleanUtils.toInteger(testResult.successes());
  }

  private static <T extends Comparable<T>> MutableText expressPropertyValue(BlockState blockState, Property<T> property) {
    final MutableText text = Text.literal("  ").append(property.getName()).append(" = ");
    final T value = blockState.get(property);
    return text.append(value instanceof Boolean bool ? Text.literal(property.name(value)).formatted(bool ? Formatting.GREEN : Formatting.RED) : Text.literal(property.name(value)));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockInfoCommandProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder) {
    return argumentBuilder
        .then(CommandManager.argument("pos", EnhancedPosArgumentType.blockPos())
            .then(CommandManager.literal("hardness")
                .executes(context -> executeGetHardness(context, 1))
                .then(CommandManager.argument("scale", FloatArgumentType.floatArg())
                    .executes(context -> executeGetHardness(context, FloatArgumentType.getFloat(context, "scale")))))
            .then(CommandManager.literal("luminance")
                .executes(TestForCommand::executeGetLuminance))
            .then(CommandManager.literal("strong_redstone_power")
                .then(CommandManager.argument("direction", DirectionArgumentType.direction())
                    .executes(TestForCommand::executeGetStrongRedstonePower)))
            .then(CommandManager.literal("weak_redstone_power")
                .then(CommandManager.argument("direction", DirectionArgumentType.direction())
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

  private static int getIntBlockInfo(CommandContext<ServerCommandSource> context, String translationKey, ToIntTriFunction<BlockState, ServerWorld, BlockPos> function) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final int value = function.applyAsInt(blockState, world, pos);
    CommandBridge.sendFeedback(source, () -> Text.translatable(translationKey, blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos), Text.literal(String.valueOf(value)).styled(TextUtil.STYLE_FOR_ACTUAL)), true);
    return value;
  }

  private static int getIntBlockInfoWithDirection(CommandContext<ServerCommandSource> context, String translationKey, ToIntQuadFunction<BlockState, ServerWorld, BlockPos, Direction> function) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final Direction direction = DirectionArgumentType.getDirection(context, "direction");
    final int value = function.applyAsInt(blockState, world, pos, direction);
    CommandBridge.sendFeedback(source, () -> Text.translatable(translationKey, blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos), Text.literal(String.valueOf(value)).styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.wrapDirection(direction).styled(TextUtil.STYLE_FOR_TARGET)), true);
    return value;
  }

  private static float getFloatBlockInfo(CommandContext<ServerCommandSource> context, String translationKey, ToFloatTriFunction<BlockState, ServerWorld, BlockPos> function) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final float value = function.applyAsFloat(blockState, world, pos);
    CommandBridge.sendFeedback(source, () -> Text.translatable(translationKey, blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos), Text.literal(String.valueOf(value)).styled(TextUtil.STYLE_FOR_ACTUAL)), true);
    return value;
  }

  private static boolean getBooleanBlockInfo(CommandContext<ServerCommandSource> context, String translationKeyWhenFalse, String translationKeyWhenTrue, TriPredicate<BlockState, ServerWorld, BlockPos> predicate) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = EnhancedPosArgumentType.getLoadedBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final BlockState blockState = world.getBlockState(pos);
    final boolean value = predicate.test(blockState, world, pos);
    CommandBridge.sendFeedback(source, () -> Text.translatable(value ? translationKeyWhenTrue : translationKeyWhenFalse, blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos)), true);
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
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.testfor.block_info.model_offset.false", blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos)), true);
      return 0;
    } else {
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.testfor.block_info.model_offset.true", blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), TextUtil.wrapVector(pos), TextUtil.wrapVector(modelOffset)), true);
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
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(ModCommands.literalR2("testfor")
        .requires(ModCommands.REQUIRES_PERMISSION_2)
        .then(addBlockCommandProperties(CommandManager.literal("block"), registryAccess))
        .then(addBlockInfoCommandProperties(CommandManager.literal("block_info"))));
  }
}
