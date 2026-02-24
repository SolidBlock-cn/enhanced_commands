package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.UnloadedPosException;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.BlockableEventLoopExtension;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionContext;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.predicate.block.AllBlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.iterator.BatchedFilterIterable;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static pers.solid.ecmd.argument.RegionArgument.region;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum FillReplaceCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final int POST_PROCESS_FLAG = 1;
  public static final int SUPPRESS_INITIAL_CHECK_FLAG = 2;
  public static final int SUPPRESS_REPLACED_CHECK_FLAG = 4;

  public static final Dynamic2CommandExceptionType REGION_TOO_LARGE = new Dynamic2CommandExceptionType((a, b) -> Component.translatable("enhanced_commands.commands.setblocks.region_too_large", a, b));
  public static final int REGION_SIZE_LIMIT = 16777215;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    LiteralArgumentBuilder<CommandSourceStack> directBuilder = literalR2("setblocks");
    LiteralArgumentBuilder<CommandSourceStack> indirectBuilder = literalR2("/setblocks");
    final KeywordArgsArgument keywordArgs = KeywordArgsArgument.builderFromShared(KeywordArgsCommon.FILLING, commandBuildContext).build();
    final LiteralCommandNode<CommandSourceStack> setBlocksNode = ModCommands.registerWithRegionArgumentModification(dispatcher, directBuilder, indirectBuilder, argument("region", region(commandBuildContext)).then(argument("block", BlockFunctionArgument.blockFunction(commandBuildContext))
        .executes(context -> execute(context, null))
        .then(argument("keyword_args", keywordArgs)
            .executes(context -> execute(context, null, KeywordArgsArgument.getKeywordArgs(context, "keyword_args")))).build()).build());

    dispatcher.register(literalR2("/s").forward(setBlocksNode.getChild("region"), ModCommands.REGION_ARGUMENTS_MODIFIER, false));
    dispatcher.register(literalR2("s").redirect(setBlocksNode));

    ModCommands.registerWithRegionArgumentModification(dispatcher,
        literalR2("replace"),
        literalR2("/replace"),
        argument("region", region(commandBuildContext))
            .then(argument("predicate", BlockPredicateArgument.blockPredicate(commandBuildContext))
                .then(argument("block", BlockFunctionArgument.blockFunction(commandBuildContext))
                    .executes(context -> {
                      final BlockPredicate blockPredicate = BlockPredicateArgument.getBlockPredicate(context, "predicate");
                      return execute(context, blockPredicate);
                    })
                    .then(argument("keyword_args", keywordArgs)
                        .executes(context -> {
                          final BlockPredicate blockPredicate = BlockPredicateArgument.getBlockPredicate(context, "predicate");
                          return execute(context, blockPredicate, KeywordArgsArgument.getKeywordArgs(context, "keyword_args"));
                        })))));
  }

  /**
   * Execute the command with the default parameters.
   */
  private static int execute(CommandContext<CommandSourceStack> context, @Nullable BlockPredicate predicate) throws CommandSyntaxException {
    return setBlocksWithDefaultKeywordArgs(RegionArgument.getRegion(context, "region"), BlockFunctionArgument.getBlockFunction(context, "block"), context.getSource(), predicate);
  }

  /**
   * Execute the command with the parameters read from args.
   */
  private static int execute(CommandContext<CommandSourceStack> context, @Nullable BlockPredicate predicate, KeywordArgs kwArgs) throws CommandSyntaxException {
    if (kwArgs.supportsArg("affect_only") && kwArgs.getArg("affect_only") instanceof BlockPredicate blockPredicate) {
      predicate = predicate == null ? blockPredicate : new AllBlockPredicate(List.of(blockPredicate, predicate));
    }
    return setBlocksFromKeywordArgs(RegionArgument.getRegion(context, "region"), BlockFunctionArgument.getBlockFunction(context, "block"), context.getSource(), predicate, kwArgs);
  }

  public static final SimpleCommandExceptionType UNLOADED_POS = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.commands.setblocks.rejected", "unloaded=" + UnloadedPosBehavior.FORCE.getSerializedName()));

  public static int setBlocksWithDefaultKeywordArgs(Region region, BlockFunction blockFunction, CommandSourceStack source, @Nullable BlockPredicate replacingTarget) throws CommandSyntaxException {
    return setBlocksInRegion(region, blockFunction, source, replacingTarget, false, false, new BlockFunctionContext(Block.UPDATE_CLIENTS, 0, source.getLevel().getRandom(), source, null), UnloadedPosBehavior.REJECT, true);
  }

  public static int setBlocksFromKeywordArgs(Region region, BlockFunction blockFunction, CommandSourceStack source, @Nullable BlockPredicate replacingTarget, KeywordArgs kwArgs) throws CommandSyntaxException {
    return setBlocksInRegion(region, blockFunction, source, replacingTarget, kwArgs.getBoolean("immediately"), kwArgs.getBoolean("bypass_limit"), new BlockFunctionContext(getFlags(kwArgs), getModFlags(kwArgs), source.getLevel().getRandom(), source, kwArgs.getArg("seed")), kwArgs.getArg("unloaded_pos"), kwArgs.getBoolean("undoable"));
  }

  public static int setBlocksInRegion(Region region, BlockFunction blockFunction, CommandSourceStack source, @Nullable BlockPredicate predicate, boolean immediately, boolean bypassLimit, BlockFunctionContext context, UnloadedPosBehavior unloadedPosBehavior, boolean undoable) throws CommandSyntaxException {
    if (!bypassLimit && region.numberOfBlocksAffected() > REGION_SIZE_LIMIT) {
      throw REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), REGION_SIZE_LIMIT);
    }
    final ServerLevel world = source.getLevel();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BoundingBox box = region.minContainingBlockBox();
      if (box != null && !LoadUtil.isPosLoaded(world, box)) {
        throw UNLOADED_POS.create();
      }
    }

    final Iterable<@NotNull BlockPos> posIterable;
    final MutableInt numbersAffected = new MutableInt();
    final MutableBoolean hasUnloaded = new MutableBoolean();

    if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      posIterable = new BatchedFilterIterable<>(region, 16, blockPos -> {
        final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) hasUnloaded.setTrue();
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
      posIterable = Iterables.transform(region, blockPos -> {
        final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) {
          hasUnloaded.setTrue();
          throw new UnloadedPosException(blockPos);
        }
        return blockPos;
      });
    } else {
      posIterable = region;
    }

    final Component taskName = Component.translatable("enhanced_commands.commands.setblocks.task_name", region.asString());
    final @Nullable BlockPlacementHistory history = undoable ? new BlockPlacementHistory(taskName, world, context.flags, context.modFlags) : null;

    // 第一部分：收集 oldStates

    final Long2ObjectMap<BlockState> oldStates = new Long2ObjectLinkedOpenHashMap<>();
    final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    final Iterable<Void> collectPosToAffect;
    if (predicate == null) {
      collectPosToAffect = Iterables.transform(posIterable, blockPos -> {
        oldStates.put(blockPos.asLong(), world.getBlockState(blockPos));
        return null;
      });
    } else {
      collectPosToAffect = Iterables.transform(posIterable, blockPos -> {
        final BlockInWorld blockInWorld = new BlockInWorld(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
        if (blockInWorld.getState() != null && predicate.test(blockInWorld, context)) {
          oldStates.put(blockPos.asLong(), blockInWorld.getState());
        }
        return null;
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
    if (!immediately && region.numberOfBlocksAffected() > 16384) {
      // The region is too large. Send a server task.
      final IteratorTask<Void> task = ((BlockableEventLoopExtension) source.getServer()).addIteratorTask$ec(taskName, Iterables.concat(
          IterateUtils.batchAndSkip(collectPosToAffect, 16384, 1),
          IterateUtils.batchAndSkip(setBlocks, 32768, 15),
          finalClaim
      ).iterator());
      if (history != null) {
        history.task = task;
      }
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.large_region", Long.toString(region.numberOfBlocksAffected())).withStyle(ChatFormatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(Iterables.concat(collectPosToAffect, setBlocks, finalClaim).iterator());
      return numbersAffected.intValue();
    }
  }

  public static int getFlags(@NotNull KeywordArgs args) {
    int value = 0;
    if (args.getBoolean("notify_listeners")) {
      value |= Block.UPDATE_CLIENTS;
    }
    if (args.getBoolean("notify_neighbors")) {
      value |= Block.UPDATE_NEIGHBORS;
    }
    if (args.getBoolean("force_state")) {
      value |= Block.UPDATE_KNOWN_SHAPE;
    }
    if (args.getBoolean("force")) {
      value |= Block.UPDATE_KNOWN_SHAPE;
      value &= ~Block.UPDATE_NEIGHBORS;
    }
    return value;
  }

  public static int getModFlags(@NotNull KeywordArgs args) {
    int value = 0;
    if (args.supportsArg("post_process") && args.getBoolean("post_process")) {
      value |= POST_PROCESS_FLAG;
    }
    if (args.getBoolean("suppress_initial_check")) {
      value |= SUPPRESS_INITIAL_CHECK_FLAG;
    }
    if (args.getBoolean("suppress_replaced_check")) {
      value |= SUPPRESS_REPLACED_CHECK_FLAG;
    }
    if (args.getBoolean("force")) {
      value |= SUPPRESS_INITIAL_CHECK_FLAG | SUPPRESS_REPLACED_CHECK_FLAG;
    }
    return value;
  }
}
