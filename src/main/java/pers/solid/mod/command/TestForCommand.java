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
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.mod.EnhancedCommands;
import pers.solid.mod.argument.BlockPredicateArgumentType;
import pers.solid.mod.argument.DirectionArgumentType;
import pers.solid.mod.predicate.block.BlockPredicate;
import pers.solid.mod.util.ToFloatTriFunction;
import pers.solid.mod.util.ToIntTriFunction;

import java.util.Collection;

public final class TestForCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
    dispatcher.register(CommandManager.literal("testfor")
        .requires(ModCommands.REQUIRES_PERMISSION_2)
        .then(addBlockCommandProperties(CommandManager.literal("block"), registryAccess))
        .then(addBlockInfoCommandProperties(CommandManager.literal("blockinfo"), registryAccess)));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockCommandProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder
        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
            .executes(TestForCommand::executesTestForBlock)
            .then(CommandManager.argument("predicate", new BlockPredicateArgumentType(registryAccess))
                .executes(context -> {
                  final ServerCommandSource source = context.getSource();
                  final TestResult testResult = context.getArgument("predicate", BlockPredicate.class).testAndDescribe(new CachedBlockPosition(source.getWorld(), BlockPosArgumentType.getBlockPos(context, "pos"), false));
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
                    .executes(TestForCommand::executeGetWeakRedstonePower))));
  }

  private static int getIntBlockInfo(CommandContext<ServerCommandSource> context, String translationKey, ToIntTriFunction<BlockState, ServerWorld, BlockPos> function) {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final int value = function.applyAsInt(world.getBlockState(pos), world, pos);
    source.sendFeedback(Text.translatable(translationKey, EnhancedCommands.wrapBlockPos(pos), Text.literal(String.valueOf(value)).styled(EnhancedCommands.STYLE_FOR_ACTUAL)), true);
    return value;
  }

  private static float getFloatBlockInfo(CommandContext<ServerCommandSource> context, String translationKey, ToFloatTriFunction<BlockState, ServerWorld, BlockPos> function) {
    final ServerCommandSource source = context.getSource();
    final BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
    final ServerWorld world = source.getWorld();
    final float value = function.applyAsFloat(world.getBlockState(pos), world, pos);
    source.sendFeedback(Text.translatable(translationKey, EnhancedCommands.wrapBlockPos(pos), Text.literal(String.valueOf(value)).styled(EnhancedCommands.STYLE_FOR_ACTUAL)), true);
    return value;
  }

  private static int executeGetHardness(CommandContext<ServerCommandSource> context, float scale) {
    final double hardness = getFloatBlockInfo(context, "enhancedCommands.testfor.blockinfo.hardness", AbstractBlock.AbstractBlockState::getHardness);
    return (int) (hardness * scale);
  }

  private static int executeGetLuminance(CommandContext<ServerCommandSource> context) {
    return getIntBlockInfo(context, "enhancedCommands.testfor.blockinfo.luminance", (blockState, serverWorld, blockPos) -> blockState.getLuminance());
  }

  private static int executeGetStrongRedstonePower(CommandContext<ServerCommandSource> context) {
    return getIntBlockInfo(context, "enhancedCommands.testfor.blocinfo.strong_redstone_power", (blockState, serverWorld, blockPos) -> blockState.getStrongRedstonePower(serverWorld, blockPos, DirectionArgumentType.getDirection(context, "direction")));
  }
  private static int executeGetWeakRedstonePower(CommandContext<ServerCommandSource> context) {
    return getIntBlockInfo(context, "enhancedCommands.testfor.blocinfo.weak_redstone_power", (blockState, serverWorld, blockPos) -> blockState.getWeakRedstonePower(serverWorld, blockPos, DirectionArgumentType.getDirection(context, "direction")));
  }
}
