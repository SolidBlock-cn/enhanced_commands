package pers.solid.ecmd.block;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.argument.KeywordArgsCommon;
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionContext;
import pers.solid.ecmd.history.BlockTransformationHistory;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.entity.EntityPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.function.Function;

public interface BlockTransformationCommand {
  static KeywordArgsArgumentType.Builder createKeywordArgs(CommandRegistryAccess registryAccess) {
    return KeywordArgsArgumentType.builderFromShared(KeywordArgsCommon.BLOCK_TRANSFORMATION, registryAccess);
  }

  Vec3i transformBlockPos(Vec3i original);

  Vec3d transformPos(Vec3d original);

  Vec3d transformPosBack(Vec3d transformed);

  void transformEntity(@NotNull Entity entity);

  void transformEntityBack(@NotNull Entity entity);

  @NotNull
  BlockState transformBlockState(@NotNull BlockState original);

  @NotNull
  Region transformRegion(@NotNull Region region);

  /**
   * 完成操作时通知影响的方块和实体的数量。
   *
   * @param affectedBlocks   影响的方块的数量。
   * @param affectedEntities 影响的实体的数量，如果命令的参数未允许影响实体，则为 -1，如果允许影响实体但是没有影响到实体，则为 1。
   */
  void notifyCompletion(ServerCommandSource source, @Range(from = 0, to = Long.MAX_VALUE) int affectedBlocks, @Range(from = -1, to = Long.MAX_VALUE) int affectedEntities);

  @NotNull
  MutableText getIteratorTaskName(Region region);

  default int execute(Region region, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final @Nullable BlockPredicate affectOnly = keywordArgs.getArg("affect_only");
    final @Nullable BlockPredicate transformOnly = keywordArgs.getArg("transform_only");
    final @Nullable BlockFunction remaining = keywordArgs.getArg("remaining");
    final ServerWorld world = source.getWorld();
    final UnloadedPosBehavior unloadedPosBehavior = keywordArgs.getArg("unloaded_pos");
    final boolean bypassLimit = keywordArgs.getArg("bypass_limit");
    final int flags = FillReplaceCommand.getFlags(keywordArgs);
    final int modFlags = FillReplaceCommand.getModFlags(keywordArgs);
    final @Nullable Long seed = keywordArgs.getArg("seed");
    final BlockTransformationHistory history = keywordArgs.getBoolean("undoable") ? new BlockTransformationHistory(getIteratorTaskName(region), world, flags, modFlags) : null;
    final BlockTransformationTask.Builder builder = BlockTransformationTask.builder(world, region)
        .setBlockPredicateContext(new ExecutionContext(world.getRandom(), source, seed))
        .setBlockFunctionContext(new BlockFunctionContext(flags, modFlags, world.getRandom(), source, seed))
        .transformsBlockPos(this::transformBlockPos)
        .transformsPos(this::transformPos)
        .transformsPosBack(this::transformPosBack)
        .transformsEntity(this::transformEntity, this::transformEntityBack)
        .transformsBlockState(keywordArgs.getBoolean("keep_state") ? Function.identity() : this::transformBlockState)
        .affectsOnly(affectOnly)
        .transformsOnly(transformOnly)
        .fillRemainingWith(remaining)
        .setUnloadedPosBehavior(unloadedPosBehavior)
        .interpolates(keywordArgs.supportsArg("interpolate") && keywordArgs.getBoolean("interpolate"))
        .bypassLimit(bypassLimit)
        .history(HistoryHolder.fromSource(source), history);
    if (keywordArgs.getBoolean("keep_remaining")) {
      builder.keepRemaining();
    }
    final EntityPredicate entitiesToAffect = keywordArgs.getArg("affect_entities");
    if (entitiesToAffect != null) {
      final ExecutionContext executionContext = new ExecutionContext(context.getSource());
      builder.entitiesToAffect(world.getEntitiesByClass(Entity.class, region.minContainingBox(), entity -> entitiesToAffect.test(entity, executionContext)).iterator());
    }

    final boolean transformsRegion = keywordArgs.getBoolean("select");
    final ServerPlayerEntity player = source.getPlayer();

    final boolean immediately = keywordArgs.getBoolean("immediately");

    final BlockTransformationTask task = builder.build();

    final @Nullable RegionSelection oldActiveRegion; // 仅用于撤销操作
    if (transformsRegion && player != null && history != null) {
      oldActiveRegion = ((ServerPlayerEntityExtension) player).getActiveRegionOrThrow$ec();
    } else {
      oldActiveRegion = null;
    }
    final RegionSelection transformedRegionSelection = oldActiveRegion != null && region.equals(oldActiveRegion.region()) ? oldActiveRegion.transformed(this::transformPos) : null;
    if (!immediately && region.numberOfBlocksAffected() > 16384) {
      final IteratorTask<Void> iteratorTask = ((ThreadExecutorExtension) source.getServer()).addIteratorTask$ec(getIteratorTaskName(region), Iterators.concat(task.transformBlocks().getSpeedAdjustedTask(), IterateUtils.singletonPeekingIterator(() -> {

        if (transformedRegionSelection != null) {
          history.reverseEntities.add(Triple.of(player, Pair.of(
              player0 -> ((ServerPlayerEntityExtension) player0).setActiveRegion$ec(oldActiveRegion),
              player0 -> ((ServerPlayerEntityExtension) player0).setActiveRegion$ec(transformedRegionSelection)
          ), null));
          ((ServerPlayerEntityExtension) player).setActiveRegion$ec(transformedRegionSelection);
        }
        notifyUnloadedPos(task, unloadedPosBehavior, source);
        notifyCompletion(source, task.getAffectedBlocks(), entitiesToAffect == null ? -1 : task.getAffectedEntities());
      })));
      history.task = iteratorTask;
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.setblocks.large_region", Long.toString(region.numberOfBlocksAffected())).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(task.transformBlocks().getImmediateTask());
      notifyUnloadedPos(task, unloadedPosBehavior, source);
      final int affectedBlocks = task.getAffectedBlocks();
      final int affectedEntities = task.getAffectedEntities();
      notifyCompletion(source, affectedBlocks, entitiesToAffect == null ? -1 : affectedEntities);
      if (transformedRegionSelection != null) {
        history.reverseEntities.add(Triple.of(player, Pair.of(
            player0 -> ((ServerPlayerEntityExtension) player0).setActiveRegion$ec(oldActiveRegion),
            player0 -> ((ServerPlayerEntityExtension) player0).setActiveRegion$ec(transformedRegionSelection)
        ), null));
        ((ServerPlayerEntityExtension) player).setActiveRegion$ec(transformedRegionSelection);
      }
      return affectedBlocks + affectedEntities;
    }
  }

  private static void notifyUnloadedPos(BlockTransformationTask task, UnloadedPosBehavior unloadedPosBehavior, ServerCommandSource source) {
    if (task.hasUnloadedPos) {
      if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.setblocks.broken").styled(Styles.ACTUAL), false);
      } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.setblocks.skipped").styled(Styles.ACTUAL), false);
      }
    }
  }
}
