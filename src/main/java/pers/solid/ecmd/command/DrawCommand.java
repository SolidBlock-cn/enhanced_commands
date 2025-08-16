package pers.solid.ecmd.command;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.curve.Curve;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionContext;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.mixins.accessor.ServerCommandSourceAccessor;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Iterator;
import java.util.stream.Stream;

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
                .executes(context -> setBlocksWithDefaultKeywordArgs(CurveArgumentType.getCurve(context, "curve"), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource(), null))
                .then(argument("kwargs", keywordArgs)
                    .executes(context -> {
                      final KeywordArgs kwargs = KeywordArgsArgumentType.getKeywordArgs(context, "kwargs");
                      return setBlocksFromKeywordArgs(CurveArgumentType.getCurve(context, "curve"), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource(), null, kwargs);
                    })))));
  }


  private static int setBlocksWithDefaultKeywordArgs(Curve curve, BlockFunction blockFunction, ServerCommandSource source, @Nullable BlockPredicate predicate) throws CommandSyntaxException {
    // todo 实现 predicate 参数
    return execute(curve, blockFunction, source, false, false, 0d, new BlockFunctionContext(Block.NOTIFY_LISTENERS, 0, source.getWorld().getRandom(), source, null), 0d, true);
  }

  private static int setBlocksFromKeywordArgs(Curve curve, BlockFunction blockFunction, ServerCommandSource source, @Nullable BlockPredicate predicate, KeywordArgs kwArgs) throws CommandSyntaxException {
    return execute(curve, blockFunction, source, kwArgs.getBoolean("immediately"), kwArgs.getBoolean("bypass_limit"), kwArgs.getArg("interval"), new BlockFunctionContext(FillReplaceCommand.getFlags(kwArgs), FillReplaceCommand.getModFlags(kwArgs), source.getWorld().getRandom(), source, kwArgs.getArg("seed")), kwArgs.getArg("thickness"), kwArgs.getBoolean("undoable"));
  }

  private static int execute(Curve curve, BlockFunction blockFunction, ServerCommandSource source, boolean immediately, boolean bypassLimit, double interval, BlockFunctionContext context, double thickness, boolean undoable) throws CommandSyntaxException {
    if (interval > 0 && interval < 0.05) interval = 0.05;
    final double estimatedIterationAmount = curve.length() / (interval == 0 ? 1 : interval) * (thickness == 0 ? 1 : Math.max(1d, MathHelper.square(thickness) * Math.PI));
    if (!Double.isFinite(estimatedIterationAmount)) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
    }
    if (!bypassLimit && estimatedIterationAmount > FillReplaceCommand.REGION_SIZE_LIMIT) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(estimatedIterationAmount, FillReplaceCommand.REGION_SIZE_LIMIT);
    }
    final ServerWorld world = source.getWorld();

    final Iterator<?> mainIterator;
    final MutableInt numbersAffected = new MutableInt();
    Stream<BlockPos> stream = interval == 0 ? curve.streamBlockPos() : curve.streamPoints(interval)
        .map(BlockPos::ofFloored)
        .distinct();

    if (thickness > 0) {
      stream = stream.flatMap(pos -> new SphereRegion(thickness, pos.toCenterPos()).stream()).distinct();
    }

    final MutableText taskName = Text.translatable("enhanced_commands.commands.draw.task_name", curve.asString());
    final @Nullable BlockPlacementHistory history = undoable ? new BlockPlacementHistory(taskName, world, context.flags, context.modFlags) : null;
    mainIterator = stream
        .peek(blockPos -> {
          if (blockFunction.setBlock(world, blockPos, context, history))
            numbersAffected.increment();
        })
        .map(blockPos -> null)
        .iterator();
    final Iterator<?> iterator = Iterators.concat(mainIterator, IterateUtils.singletonPeekingIterator(() -> source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.setblocks.complete", numbersAffected.getValue()).enhanced$$(), true)));


    if (history != null) {
      if (((ServerCommandSourceAccessor) source).getOutput() instanceof HistoryHolder historyHolder) {
        historyHolder.addUndoableHistory$ec(history);
      }
    }

    if (!immediately && estimatedIterationAmount > 16384) {
      // The region is too large. Send a server task.
      final IteratorTask<?> task = ((ThreadExecutorExtension) source.getServer()).addIteratorTask$ec(taskName, IterateUtils.batchAndSkip(iterator, 32768, 15));
      if (history != null) {
        history.task = task;
      }
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.setblocks.large_region", estimatedIterationAmount).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(iterator);
      return numbersAffected.intValue();
    }
  }
}
