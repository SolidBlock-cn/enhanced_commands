package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.BlockFunctionContext;
import pers.solid.ecmd.block.predicate.AllBlockPredicate;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.config.BlockOperationConfig;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.task.BlockPlacementTask;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.extension.BlockableEventLoopExtension;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static pers.solid.ecmd.argument.RegionArgument.region;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum SetReplaceBlocksCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;
  public static final int POST_PROCESS_FLAG = 1;
  public static final int SUPPRESS_INITIAL_CHECK_FLAG = 2;
  public static final int SUPPRESS_REPLACED_CHECK_FLAG = 4;

  public static final Dynamic2CommandExceptionType REGION_TOO_LARGE = new Dynamic2CommandExceptionType((a, b) -> Component.translatable("enhanced_commands.commands.setblocks.region_too_large", a, b));

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    LiteralArgumentBuilder<CommandSourceStack> directBuilder = literalR2("setblocks");
    LiteralArgumentBuilder<CommandSourceStack> indirectBuilder = literalR2("/setblocks");
    final KeywordArgsArgument keywordArgs = KeywordArgsArgument.builderFromShared(KeywordArgsCommon.FILLING, commandBuildContext).build();
    final LiteralCommandNode<CommandSourceStack> setBlocksNode = EnhancedCommandsCommands.registerWithRegionArgumentModification(dispatcher, directBuilder, indirectBuilder, argument("region", region(commandBuildContext)).then(argument("block", BlockFunctionArgument.blockFunction(commandBuildContext))
        .executes(context -> execute(context, null))
        .then(argument("keyword_args", keywordArgs)
            .executes(context -> execute(context, null, KeywordArgsArgument.getKeywordArgs(context, "keyword_args")))).build()).build());

    dispatcher.register(literalR2("/s").forward(setBlocksNode.getChild("region"), EnhancedCommandsCommands.REGION_ARGUMENTS_MODIFIER, false));
    dispatcher.register(literalR2("s").redirect(setBlocksNode));

    EnhancedCommandsCommands.registerWithRegionArgumentModification(dispatcher,
        literalR2("replaceblocks"),
        literalR2("/replaceblocksblocks"),
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
    return setBlocksInRegion(region, blockFunction, source, replacingTarget, kwArgs.getBoolean("immediately"), kwArgs.getBoolean("bypass_limit"), new BlockFunctionContext(getFlags(kwArgs), getModFlags(kwArgs), source.getLevel().getRandom(), source, kwArgs.getArg("seed")), kwArgs.getRequiredArg("unloaded_pos"), kwArgs.getBoolean("undoable"));
  }

  public static int setBlocksInRegion(Region region, BlockFunction blockFunction, CommandSourceStack source, @Nullable BlockPredicate predicate, boolean immediately, boolean bypassLimit, BlockFunctionContext context, UnloadedPosBehavior unloadedPosBehavior, boolean undoable) throws CommandSyntaxException {
    final int regionSizeLimit = BlockOperationConfig.current.regionSizeLimit;
    if (!bypassLimit && region.numberOfBlocksAffected() > regionSizeLimit) {
      throw REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), regionSizeLimit);
    }
    final ServerLevel world = source.getLevel();
    immediately = immediately || region.numberOfBlocksAffected() <= 16384;
    // reject 操作是在创建 task 之前就进行的。
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BoundingBox box = region.minContainingBlockBox();
      if (box != null && !LoadUtil.isPosLoaded(world, box)) {
        throw UNLOADED_POS.create();
      }
    }

    final Component taskName = Component.translatable("enhanced_commands.commands.setblocks.task_name", region.expressAsString());

    final @Nullable BlockPlacementHistory history = undoable ? new BlockPlacementHistory(taskName, world, context.flags, context.modFlags) : null;

    final BlockPlacementTask task = BlockPlacementTask.builder(taskName, Mth.createInsecureUUID(world.getRandom()), source)
        .blockFunctionContext(context)
        .blockFunction(blockFunction)
        .blockPredicate(predicate)
        .immediately(immediately)
        .region(region)
        .undoable(undoable)
        .unloadedPosBehavior(unloadedPosBehavior)
        .world(world)
        .build();

    if (!immediately) {
      ((BlockableEventLoopExtension) source.getServer()).addIteratorTask$ec(task);
      if (history != null) {
        history.task = task;
      }
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.large_region", Long.toString(region.numberOfBlocksAffected())).withStyle(ChatFormatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaustCommand(task);
      return task.numbersAffected;
    }
  }

  public static int getFlags(KeywordArgs args) {
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

  public static int getModFlags(KeywordArgs args) {
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
