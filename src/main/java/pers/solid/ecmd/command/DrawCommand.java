package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.UnloadedPosException;
import pers.solid.ecmd.curve.Curve;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionContext;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.iterator.BatchedFilterIterable;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.stream.LongStream;

import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum DrawCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgs = KeywordArgsArgumentType.builder()
        .addShared(KeywordArgsCommon.FILLING, registryAccess)
        .addOptionalArg("interval", DoubleArgumentType.doubleArg(0d), 0d)
        .addOptionalArg("thickness", DoubleArgumentType.doubleArg(0d, 64d), 0d)
        .build();
    dispatcher.register(literalR2("draw")
        .then(argument("curve", CurveArgumentType.curve(registryAccess))
            .then(argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
                .executes(context -> setBlocksWithDefaultKeywordArgs(CurveArgumentType.getCurve(context, "curve"), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource()))
                .then(argument("kwargs", keywordArgs)
                    .executes(context -> {
                      final KeywordArgs kwargs = KeywordArgsArgumentType.getKeywordArgs(context, "kwargs");
                      return setBlocksFromKeywordArgs(CurveArgumentType.getCurve(context, "curve"), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource(), kwargs);
                    })))));
  }


  private static int setBlocksWithDefaultKeywordArgs(Curve curve, BlockFunction blockFunction, ServerCommandSource source) throws CommandSyntaxException {
    return execute(curve, blockFunction, source, null, false, false, 0d, new BlockFunctionContext(Block.NOTIFY_LISTENERS, 0, source.getWorld().getRandom(), source, null), 0d, UnloadedPosBehavior.REJECT, true);
  }

  private static int setBlocksFromKeywordArgs(Curve curve, BlockFunction blockFunction, ServerCommandSource source, KeywordArgs kwArgs) throws CommandSyntaxException {
    return execute(curve, blockFunction, source, kwArgs.getArg("affect_only"), kwArgs.getBoolean("immediately"), kwArgs.getBoolean("bypass_limit"), kwArgs.getArg("interval"), new BlockFunctionContext(FillReplaceCommand.getFlags(kwArgs), FillReplaceCommand.getModFlags(kwArgs), source.getWorld().getRandom(), source, kwArgs.getArg("seed")), kwArgs.getArg("thickness"), kwArgs.getArg("unloaded_pos"), kwArgs.getBoolean("undoable"));
  }

  private static int execute(Curve curve, BlockFunction blockFunction, ServerCommandSource source, @Nullable BlockPredicate predicate, boolean immediately, boolean bypassLimit, double interval, BlockFunctionContext context, double thickness, UnloadedPosBehavior unloadedPosBehavior, boolean undoable) throws CommandSyntaxException {
    if (interval > -0.05 && interval < 0.05) interval = 0.05;
    final double estimatedIterationAmount = curve.length() / Math.min(interval, 1) * (thickness == 0 ? 1 : Math.max(1d, MathHelper.square(thickness) * Math.PI));
    if (!Double.isFinite(estimatedIterationAmount)) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
    }
    if (!bypassLimit && estimatedIterationAmount > FillReplaceCommand.REGION_SIZE_LIMIT) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(estimatedIterationAmount, FillReplaceCommand.REGION_SIZE_LIMIT);
    }

    final ServerWorld world = source.getWorld();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final @Nullable Box box = curve.minContainingBox();
      if (box != null && !LoadUtil.isPosLoaded(world, box)) {
        throw FillReplaceCommand.UNLOADED_POS.create();
      }
    }

    // 尽可能使用 mutable，避免每次都创建对象；在构造流的过程中，转化为 long 是为了能够执行 distinct
    // 请注意：每次迭代时，可能都是同一个对象。
    LongStream longStream;
    BlockPos.Mutable mutable = new BlockPos.Mutable();
    final Double i = interval;
    if (thickness > 0) {
      longStream = curve.streamPoints(i).flatMapToLong(vec3d -> LongStream.concat(LongStream.of(mutable.set(vec3d.x, vec3d.y, vec3d.z).asLong()), new SphereRegion(thickness, vec3d).stream().mapToLong(BlockPos::asLong)).distinct().sequential()).distinct();
    } else {
      longStream = curve.streamPoints(i).mapToLong(value -> mutable.set(value.x, value.y, value.z).asLong()).distinct();
    }
    Iterable<BlockPos.Mutable> posIterable = () -> IterateUtils.transformLongToObject(longStream.iterator(), mutable::set);

    final MutableInt numbersAffected = new MutableInt();
    final MutableBoolean hasUnloaded = new MutableBoolean();

    if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      posIterable = new BatchedFilterIterable<>(posIterable, 16, blockPos -> {
        final boolean chunkLoaded = world.isChunkLoaded(blockPos);
        if (!chunkLoaded) hasUnloaded.setTrue();
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
      posIterable = Iterables.transform(posIterable, blockPos -> {
        final boolean chunkLoaded = world.isChunkLoaded(blockPos);
        if (!chunkLoaded) {
          hasUnloaded.setTrue();
          throw new UnloadedPosException(blockPos);
        }
        return blockPos;
      });
    }

    final MutableText taskName = Text.translatable("enhanced_commands.commands.draw.task_name", curve.asString());
    final @Nullable BlockPlacementHistory history = undoable ? new BlockPlacementHistory(taskName, world, context.flags, context.modFlags) : null;


    // 第一部分：收集 oldStates

    final Long2ObjectMap<BlockState> oldStates = new Long2ObjectLinkedOpenHashMap<>();
    final Iterable<Void> collectPosToAffect;
    if (predicate == null) {
      collectPosToAffect = Iterables.transform(posIterable, blockPos -> {
        if (blockPos == null) return null;
        oldStates.put(blockPos.asLong(), world.getBlockState(blockPos));
        return (Void) null;
      });
    } else {
      collectPosToAffect = Iterables.transform(posIterable, blockPos -> {
        if (blockPos == null) return null;
        final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
        if (cachedBlockPosition.getBlockState() != null && predicate.test(cachedBlockPosition, context)) {
          oldStates.put(blockPos.asLong(), cachedBlockPosition.getBlockState());
        }
        return (Void) null;
      });
    }

    // 第二部分：放置方块

    final Iterable<Void> setBlocks = Iterables.transform(oldStates.long2ObjectEntrySet(), entry -> {
      if (blockFunction.setBlock(world, mutable.set(entry.getLongKey()), context, entry.getValue(), history)) {
        numbersAffected.increment();
      }
      return null;
    });

    // 第三部分：结束时声明

    final Iterable<Void> finalClaim = IterateUtils.singletonPeekingIterable(() -> source.sendFeedback$ecBridge(() -> hasUnloaded.getValue() ? switch (unloadedPosBehavior) {
      case SKIP -> Text.translatable("enhanced_commands.commands.setblocks.complete_skipped", numbersAffected.intValue());
      case BREAK -> Text.translatable("enhanced_commands.commands.setblocks.complete_broken", numbersAffected.intValue());
      default -> Text.translatable("enhanced_commands.commands.setblocks.complete", numbersAffected.intValue());
    } : Text.translatable("enhanced_commands.commands.setblocks.complete", numbersAffected.intValue()).enhanced$$(), true));


    if (history != null) {
      final HistoryHolder historyHolder = HistoryHolder.fromSource(source);
      if (historyHolder != null) {
        historyHolder.addUndoableHistory$ec(history);
      }
    }

    if (!immediately && estimatedIterationAmount > 16384) {
      // The region is too large. Send a server task.
      final IteratorTask<Void> task = ((ThreadExecutorExtension) source.getServer()).addIteratorTask$ec(taskName, Iterables.concat(
          IterateUtils.batchAndSkip(collectPosToAffect, 16384, 1),
          IterateUtils.batchAndSkip(setBlocks, 32768, 15),
          finalClaim
      ).iterator());
      if (history != null) {
        history.task = task;
      }
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.setblocks.large_region", Double.toString(estimatedIterationAmount)).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(Iterables.concat(collectPosToAffect, setBlocks, finalClaim).iterator());
      return numbersAffected.intValue();
    }
  }
}
