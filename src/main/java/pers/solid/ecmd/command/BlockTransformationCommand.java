package pers.solid.ecmd.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgument;
import pers.solid.ecmd.argument.KeywordArgsCommon;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.BlockFunctionContext;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.entity.predicate.EntityPredicate;
import pers.solid.ecmd.history.BlockTransformationHistory;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.task.BlockTransformationTask;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.extension.BlockableEventLoopExtension;
import pers.solid.ecmd.util.extension.HistoryHolder;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.function.Function;

public interface BlockTransformationCommand {
  static KeywordArgsArgument.Builder createKeywordArgs(CommandBuildContext commandBuildContext) {
    return KeywordArgsArgument.builderFromShared(KeywordArgsCommon.BLOCK_TRANSFORMATION, commandBuildContext);
  }

  Vec3i transformBlockPos(Vec3i original);

  Vec3 transformPos(Vec3 original);

  Vec3 transformPosBack(Vec3 transformed);

  void transformEntity(Entity entity);

  void transformEntityBack(Entity entity);

  BlockState transformBlockState(BlockState original);

  Region transformRegion(Region region);

  /**
   * 完成操作时通知影响的方块和实体的数量。
   *
   * @param affectedBlocks   影响的方块的数量。
   * @param affectedEntities 影响的实体的数量，如果命令的参数未允许影响实体，则为 -1，如果允许影响实体但是没有影响到实体，则为 1。
   */
  void notifyCompletion(CommandSourceStack source, @Range(from = 0, to = Long.MAX_VALUE) int affectedBlocks, @Range(from = -1, to = Long.MAX_VALUE) int affectedEntities);

  MutableComponent getIteratorTaskName(Region region);

  default int execute(Region region, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final @Nullable BlockPredicate affectOnly = keywordArgs.getArg("affect_only");
    final @Nullable BlockPredicate transformOnly = keywordArgs.getArg("transform_only");
    final @Nullable BlockFunction remaining = keywordArgs.getArg("remaining");
    final ServerLevel world = source.getLevel();
    final UnloadedPosBehavior unloadedPosBehavior = keywordArgs.getRequiredArg("unloaded_pos");
    final boolean bypassLimit = keywordArgs.getBoolean("bypass_limit");
    final int flags = SetReplaceBlocksCommand.getFlags(keywordArgs);
    final int modFlags = SetReplaceBlocksCommand.getModFlags(keywordArgs);
    final @Nullable Long seed = keywordArgs.getArg("seed");
    final MutableComponent iteratorTaskName = getIteratorTaskName(region);
    final @Nullable BlockTransformationHistory history = keywordArgs.getBoolean("undoable") ? new BlockTransformationHistory(iteratorTaskName, world, flags, modFlags) : null;
    final boolean shouldTransformRegion = keywordArgs.getBoolean("select");
    final @Nullable EntityPredicate entitiesToAffect = keywordArgs.getArg("affect_entities");
    final boolean immediately = keywordArgs.getBoolean("immediately") || region.numberOfBlocksAffected() <= 16384;

    final BlockTransformationTask.Builder builder = BlockTransformationTask.builder(world, region, iteratorTaskName, Mth.createInsecureUUID(world.getRandom()), source)
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
        .history(HistoryHolder.fromSource(source), history)
        .shouldTransformRegion(shouldTransformRegion)
        .notifiesCompletion(blockTransformationTask -> notifyCompletion(source, blockTransformationTask.getAffectedBlocks(), entitiesToAffect == null ? -1 : blockTransformationTask.getAffectedEntities()))
        .immediately(immediately);
    if (keywordArgs.getBoolean("keep_remaining")) {
      builder.keepRemaining();
    }
    if (entitiesToAffect != null) {
      final ExecutionContext executionContext = new ExecutionContext(context.getSource());
      builder.entitiesToAffect(world.getEntitiesOfClass(Entity.class, region.minContainingBox(), entity -> entitiesToAffect.test(entity, executionContext)).iterator());
    }

    final BlockTransformationTask task = builder.build();

    if (!immediately) {
      ((BlockableEventLoopExtension) source.getServer()).addIteratorTask$ec(task);
      if (history != null) {
        history.task = task;
      }
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.large_region", Long.toString(region.numberOfBlocksAffected())).withStyle(ChatFormatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaustCommand(task);
      return task.getAffectedBlocks() + task.getAffectedEntities();
    }
  }
}
