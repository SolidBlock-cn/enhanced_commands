package pers.solid.ecmd.command;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockRotationArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.build.BlockTransformationTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import static net.minecraft.command.argument.BlockRotationArgumentType.blockRotation;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.EnhancedPosArgumentType.getBlockPos;
import static pers.solid.ecmd.argument.RegionArgumentType.getRegion;
import static pers.solid.ecmd.argument.RegionArgumentType.region;

public enum RotateCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgs = KeywordArgsArgumentType.builder(FillReplaceCommand.KEYWORD_ARGS)
        .addOptionalArg("affects", BlockPredicateArgumentType.blockPredicate(registryAccess), null)
        .addOptionalArg("transforms", BlockPredicateArgumentType.blockPredicate(registryAccess), null)
        .addOptionalArg("remains", BlockFunctionArgumentType.blockFunction(registryAccess), BlockTransformationTask.DEFAULT_REMAINING_FUNCTION)
        .addOptionalArg("keeps", BoolArgumentType.bool(), false)
        // TODO: 2023年10月25日 implement unloaded pos behavior
        .build();
    final KeywordArgsArgumentType keywordArgsWithSlash = KeywordArgsArgumentType.builder(keywordArgs)
        .addOptionalArg("transforms_region", BoolArgumentType.bool(), false)
        .build();

    dispatcher.register(literal("rotate")
        .then(argument("region", region(registryAccess))
            .then(argument("rotation", blockRotation())
                .executes(context -> executesRotate(getRegion(context, "region"), EnhancedPosArgumentType.CURRENT_POS.toAbsoluteBlockPos(context.getSource()), keywordArgs.defaultArgs(), context))
                .then(argument("pivot", EnhancedPosArgumentType.blockPos())
                    .executes(context -> executesRotate(getRegion(context, "region"), getBlockPos(context, "pivot"), keywordArgs.defaultArgs(), context))
                    .then(argument("keyword_args", keywordArgs)
                        .executes(context -> executesRotate(getRegion(context, "region"), getBlockPos(context, "pivot"), KeywordArgsArgumentType.getKeywordArgs("keyword_args", context), context)))))));
    dispatcher.register(literal("/rotate")
        .then(argument("rotation", blockRotation())
            .executes(context -> executesRotate(((ServerPlayerEntityExtension) context.getSource().getPlayerOrThrow()).ec$getOrEvaluateActiveRegion(context.getSource()), EnhancedPosArgumentType.CURRENT_POS.toAbsoluteBlockPos(context.getSource()), keywordArgs.defaultArgs(), context))
            .then(argument("pivot", EnhancedPosArgumentType.blockPos())
                .executes(context -> executesRotate(((ServerPlayerEntityExtension) context.getSource().getPlayerOrThrow()).ec$getOrEvaluateActiveRegion(context.getSource()), getBlockPos(context, "pivot"), keywordArgsWithSlash.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgsWithSlash)
                    .executes(context -> executesRotate(((ServerPlayerEntityExtension) context.getSource().getPlayerOrThrow()).ec$getOrEvaluateActiveRegion(context.getSource()), getBlockPos(context, "pivot"), KeywordArgsArgumentType.getKeywordArgs("keyword_args", context), context))))));
  }

  public static int executesRotate(Region region, BlockPos pivot, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) {
    final ServerCommandSource source = context.getSource();
    final BlockRotation rotation = BlockRotationArgumentType.getBlockRotation(context, "rotation");
    final BlockTransformationTask.Builder builder = BlockTransformationTask.builder(source.getWorld(), region)
        .setFlags(FillReplaceCommand.getFlags(keywordArgs))
        .setModFlags(FillReplaceCommand.getModFlags(keywordArgs))
        .transformsBlockPos(vec3i -> GeoUtil.rotate(vec3i, rotation, pivot))
        .transformsPos(vec3d -> GeoUtil.rotate(vec3d, rotation, pivot.toCenterPos()))
        .transformsEntity(entity -> entity.setYaw(entity.applyRotation(rotation)))
        .transformsBlockState(blockState -> blockState.rotate(rotation))
        .affectsOnly(keywordArgs.getArg("affects"))
        .transformsOnly(keywordArgs.getArg("transforms"))
        .fillRemainingWith(keywordArgs.getArg("remains"));
    if (keywordArgs.getBoolean("keeps")) {
      builder.keepRemaining();
    }

    final boolean transformsRegion = keywordArgs.supportsArg("transforms_region") && keywordArgs.getBoolean("transforms_region");
    final ServerPlayerEntity player = source.getPlayer();

    final boolean immediately = keywordArgs.getBoolean("immediately");

    RegionBuilder regionBuilder = null;
    RegionArgument<?> activeRegion = null;
    if (transformsRegion && player != null) {
      try {
        regionBuilder = ((ServerPlayerEntityExtension) player).ec$getRegionBuilder();
        if (regionBuilder != null) {
          regionBuilder = regionBuilder.clone();
          regionBuilder.rotate(rotation, pivot.toCenterPos());
        }
      } catch (RuntimeException e) {
        regionBuilder = null;
      }
      try {
        activeRegion = ((ServerPlayerEntityExtension) player).ec$getActiveRegion();
        if (activeRegion != null) {
          activeRegion = activeRegion.toAbsoluteRegion(source).rotated(rotation, pivot.toCenterPos());
        }
      } catch (RuntimeException e) {
        activeRegion = null;
      }
    }

    final BlockTransformationTask task = builder.build();
    if (immediately && region.numberOfBlocksAffected() > 16384) {
      RegionBuilder finalRegionBuilder = regionBuilder;
      RegionArgument<?> finalActiveRegion = activeRegion;
      ((ThreadExecutorExtension) source.getServer()).ec_addIteratorTask(Text.translatable("enhancedCommands.commands.rotate.task", region.asString()), Iterators.concat(task.transformBlocks().getSpeedAdjustedTask(), IterateUtils.singletonPeekingIterator(() -> {
        if (finalRegionBuilder != null) {
          ((ServerPlayerEntityExtension) player).ec$switchRegionBuilder(finalRegionBuilder);
        }
        if (finalActiveRegion != null) {
          ((ServerPlayerEntityExtension) player).ec$setActiveRegion(finalActiveRegion);
        }
        CommandBridge.sendFeedback(source, () -> TextUtil.enhancedTranslatable("enhancedCommands.commands.rotate.complete", Integer.toString(task.getAffectedBlocks())), true);
      })));
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.fill.large_region", Long.toString(region.numberOfBlocksAffected())).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(task.transformBlocks().getImmediateTask());
      final int affectedBlocks = task.getAffectedBlocks();
      CommandBridge.sendFeedback(source, () -> TextUtil.enhancedTranslatable("enhancedCommands.commands.rotate.complete", Integer.toString(affectedBlocks)), true);
      if (transformsRegion && player != null) {
        if (activeRegion != null) {
          ((ServerPlayerEntityExtension) player).ec$setActiveRegion(activeRegion);
        }
        if (regionBuilder != null) {
          ((ServerPlayerEntityExtension) player).ec$setRegionBuilder(regionBuilder);
        }
      }
      return affectedBlocks;
    }
  }
}
