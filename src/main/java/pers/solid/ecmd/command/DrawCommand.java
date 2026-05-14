package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.UnloadedPosException;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.BlockFunctionContext;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.config.BlockOperationConfig;
import pers.solid.ecmd.curve.Curve;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.extension.BlockableEventLoopExtension;
import pers.solid.ecmd.util.extension.HistoryHolder;
import pers.solid.ecmd.util.iterator.BatchedFilterIterable;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.iterator.IteratorTask;

import java.util.Collections;
import java.util.stream.LongStream;

import static net.minecraft.commands.Commands.argument;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum DrawCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgument keywordArgs = KeywordArgsArgument.builder()
        .addShared(KeywordArgsCommon.FILLING, commandBuildContext)
        .addOptionalArg("interval", DoubleArgumentType.doubleArg(0d), 0d)
        .addOptionalArg("thickness", DoubleArgumentType.doubleArg(0d, 64d), 0d)
        .build();
    dispatcher.register(literalR2("draw")
        .then(argument("curve", CurveArgument.curve(commandBuildContext))
            .then(argument("block", BlockFunctionArgument.blockFunction(commandBuildContext))
                .executes(context -> setBlocksWithDefaultKeywordArgs(CurveArgument.getCurve(context, "curve"), BlockFunctionArgument.getBlockFunction(context, "block"), context.getSource()))
                .then(argument("kwargs", keywordArgs)
                    .executes(context -> {
                      final KeywordArgs kwargs = KeywordArgsArgument.getKeywordArgs(context, "kwargs");
                      return setBlocksFromKeywordArgs(CurveArgument.getCurve(context, "curve"), BlockFunctionArgument.getBlockFunction(context, "block"), context.getSource(), kwargs);
                    })))));
  }


  private static int setBlocksWithDefaultKeywordArgs(Curve curve, BlockFunction blockFunction, CommandSourceStack source) throws CommandSyntaxException {
    return execute(curve, blockFunction, source, null, false, false, 0d, new BlockFunctionContext(Block.UPDATE_CLIENTS, 0, source.getLevel().getRandom(), source, null), 0d, UnloadedPosBehavior.REJECT, true);
  }

  private static int setBlocksFromKeywordArgs(Curve curve, BlockFunction blockFunction, CommandSourceStack source, KeywordArgs kwArgs) throws CommandSyntaxException {
    return execute(curve, blockFunction, source, kwArgs.getArg("affect_only"), kwArgs.getBoolean("immediately"), kwArgs.getBoolean("bypass_limit"), kwArgs.getArg("interval"), new BlockFunctionContext(FillReplaceCommand.getFlags(kwArgs), FillReplaceCommand.getModFlags(kwArgs), source.getLevel().getRandom(), source, kwArgs.getArg("seed")), kwArgs.getArg("thickness"), kwArgs.getArg("unloaded_pos"), kwArgs.getBoolean("undoable"));
  }

  private static int execute(Curve curve, BlockFunction blockFunction, CommandSourceStack source, @Nullable BlockPredicate predicate, boolean immediately, boolean bypassLimit, double interval, BlockFunctionContext context, double thickness, UnloadedPosBehavior unloadedPosBehavior, boolean undoable) throws CommandSyntaxException {
    if (interval > -0.05 && interval < 0.05) interval = 0.05;
    final double estimatedIterationAmount = curve.length() / Math.min(interval, 1) * (thickness == 0 ? 1 : Math.max(1d, Mth.square(thickness) * Math.PI));
    if (!Double.isFinite(estimatedIterationAmount)) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
    }
    final int regionSizeLimit = BlockOperationConfig.current.regionSizeLimit;
    if (!bypassLimit && estimatedIterationAmount > regionSizeLimit) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(estimatedIterationAmount, regionSizeLimit);
    }

    final ServerLevel world = source.getLevel();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final @Nullable AABB box = curve.minContainingBox();
      if (box != null && !LoadUtil.isPosLoaded(world, box)) {
        throw FillReplaceCommand.UNLOADED_POS.create();
      }
    }

    // 尽可能使用 mutable，避免每次都创建对象；在构造流的过程中，转化为 long 是为了能够执行 distinct
    // 请注意：每次迭代时，可能都是同一个对象。
    LongStream longStream;
    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    final Double i = interval;
    if (thickness > 0) {
      longStream = curve.streamPoints(i).flatMapToLong(vec3d -> LongStream.concat(LongStream.of(mutable.set(vec3d.x, vec3d.y, vec3d.z).asLong()), new SphereRegion(thickness, vec3d).stream().mapToLong(BlockPos::asLong)).distinct().sequential()).distinct();
    } else {
      longStream = curve.streamPoints(i).mapToLong(value -> mutable.set(value.x, value.y, value.z).asLong()).distinct();
    }
    Iterable<BlockPos.@Nullable MutableBlockPos> posIterable = () -> IterateUtils.transformLongToObject(longStream.iterator(), mutable::set);

    final MutableInt numbersAffected = new MutableInt();
    final MutableBoolean hasUnloaded = new MutableBoolean();

    if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      posIterable = new BatchedFilterIterable<>(posIterable, 16, blockPos -> {
        @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) hasUnloaded.setTrue();
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
      posIterable = Iterables.transform(posIterable, blockPos -> {
        if (blockPos == null) {
          return null;
        }
        @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) {
          hasUnloaded.setTrue();
          throw new UnloadedPosException(blockPos);
        }
        return blockPos;
      });
    }

    final MutableComponent taskName = Component.translatable("enhanced_commands.commands.draw.task_name", curve.expressAsString());
    final @Nullable BlockPlacementHistory history = undoable ? new BlockPlacementHistory(taskName, world, context.flags, context.modFlags) : null;


    // 第一部分：收集 oldStates

    final Long2ObjectMap<BlockState> oldStates = new Long2ObjectLinkedOpenHashMap<>();
    final Iterable<FailableRunnable<Throwable>> collectPosToAffect;
    if (predicate == null) {
      collectPosToAffect = Iterables.transform(posIterable, blockPos -> () -> {
        if (blockPos == null) return;
        oldStates.put(blockPos.asLong(), world.getBlockState(blockPos));
      });
    } else {
      collectPosToAffect = Iterables.transform(posIterable, blockPos -> () -> {
        if (blockPos == null) return;
        final BlockInWorld blockInWorld = new BlockInWorld(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
        if (blockInWorld.getState() != null && predicate.test(blockInWorld, context)) {
          oldStates.put(blockPos.asLong(), blockInWorld.getState());
        }
      });
    }

    // 第二部分：放置方块

    final Iterable<FailableRunnable<Throwable>> setBlocks = Iterables.transform(oldStates.long2ObjectEntrySet(), entry -> () -> {
      try {
        if (blockFunction.setBlock(world, mutable.set(entry.getLongKey()), context, entry.getValue(), history)) {
          numbersAffected.increment();
        }
      } catch (CommandSyntaxException e) {
        throw new CommandRuntimeException(e);
      }
    });

    // 第三部分：结束时声明

    final Iterable<FailableRunnable<Throwable>> finalClaim = Collections.singleton(() -> source.sendFeedback$ecBridge(() -> hasUnloaded.getValue() ? switch (unloadedPosBehavior) {
      case SKIP -> Component.translatable("enhanced_commands.commands.setblocks.complete_skipped", numbersAffected.intValue());
      case BREAK -> Component.translatable("enhanced_commands.commands.setblocks.complete_broken", numbersAffected.intValue());
      default -> Component.translatable("enhanced_commands.commands.setblocks.complete", numbersAffected.intValue());
    } : Component.translatable("enhanced_commands.commands.setblocks.complete", numbersAffected.intValue()).enhanced$$(), true));


    if (history != null) {
      final HistoryHolder historyHolder = HistoryHolder.fromSource(source);
      if (historyHolder != null) {
        historyHolder.addUndoableHistory$ec(history);
      }
    }

    if (!immediately && estimatedIterationAmount > 16384) {
      // The region is too large. Send a server task.
      final Iterable<@Nullable FailableRunnable<Throwable>> a = IterateUtils.batchAndSkip(collectPosToAffect, 16384, 1);
      final Iterable<@Nullable FailableRunnable<Throwable>> b = IterateUtils.batchAndSkip(setBlocks, 32768, 15);
      final IteratorTask task = ((BlockableEventLoopExtension) source.getServer()).addIteratorTask$ec(taskName, Iterables.concat(
          a,
          b,
          finalClaim
      ).iterator(), source);
      if (history != null) {
        history.task = task;
      }
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.large_region", Double.toString(estimatedIterationAmount)).withStyle(ChatFormatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaustCommand(Iterables.concat(collectPosToAffect, setBlocks, finalClaim).iterator());
      return numbersAffected.intValue();
    }
  }
}
