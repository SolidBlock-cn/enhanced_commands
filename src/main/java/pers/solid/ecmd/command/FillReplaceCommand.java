package pers.solid.ecmd.command;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.UnloadedPosBehavior;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.argument.RegionArgumentType.region;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum FillReplaceCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final int POST_PROCESS_FLAG = 1;
  public static final int SUPPRESS_INITIAL_CHECK_FLAG = 2;
  public static final int SUPPRESS_REPLACED_CHECK_FLAG = 4;

  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("immediately", BoolArgumentType.bool(), false)
      .addOptionalArg("bypass_limit", BoolArgumentType.bool(), false)
      .addOptionalArg("skip_light_update", BoolArgumentType.bool(), false)
      .addOptionalArg("notify_listeners", BoolArgumentType.bool(), true)
      .addOptionalArg("notify_neighbors", BoolArgumentType.bool(), false)
      .addOptionalArg("force_state", BoolArgumentType.bool(), false)
      .addOptionalArg("post_process", BoolArgumentType.bool(), false)
      .addOptionalArg("unloaded_pos", new UnloadedPosBehaviorArgumentType(), UnloadedPosBehavior.REJECT)
      .addOptionalArg("suppress_initial_check", BoolArgumentType.bool(), false)
      .addOptionalArg("suppress_replaced_check", BoolArgumentType.bool(), false)
      .addOptionalArg("force", BoolArgumentType.bool(), false)
      .build();
  public static final Dynamic2CommandExceptionType REGION_TOO_LARGE = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.commands.fill.region_too_large", a, b));
  public static final int REGION_SIZE_LIMIT = 16777215;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    LiteralArgumentBuilder<ServerCommandSource> directBuilder = literalR2("fill");
    LiteralArgumentBuilder<ServerCommandSource> indirectBuilder = literalR2("/fill");
    final LiteralCommandNode<ServerCommandSource> fillNode = ModCommands.registerWithRegionArgumentModification(dispatcher, directBuilder, indirectBuilder, argument("region", region(registryAccess)).then(argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
        .executes(context -> execute(context, null))
        .then(argument("keyword_args", KEYWORD_ARGS)
            .executes(context -> execute(context, null, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args")))).build()).build());

    dispatcher.register(literalR2("/f").forward(fillNode.getChild("region"), ModCommands.REGION_ARGUMENTS_MODIFIER, false));

    ModCommands.registerWithRegionArgumentModification(dispatcher,
        literalR2("replace"),
        literalR2("/replace"),
        argument("region", region(registryAccess))
            .then(argument("predicate", BlockPredicateArgumentType.blockPredicate(registryAccess))
                .then(argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
                    .executes(context -> execute(context, cachedBlockPosition -> BlockPredicateArgumentType.getBlockPredicate(context, "predicate").test(cachedBlockPosition)))
                    .then(argument("keyword_args", KEYWORD_ARGS)
                        .executes(context -> execute(context, cachedBlockPosition1 -> BlockPredicateArgumentType.getBlockPredicate(context, "predicate").test(cachedBlockPosition1), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args")))))));
  }

  /**
   * Execute the command with the default parameters.
   */
  private static int execute(CommandContext<ServerCommandSource> context, Predicate<CachedBlockPosition> predicate) throws CommandSyntaxException {
    return setBlocksWithDefaultKeywordArgs(RegionArgumentType.getRegion(context, "region"), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource(), predicate);
  }

  /**
   * Execute the command with the parameters read from args.
   */
  private static int execute(CommandContext<ServerCommandSource> context, Predicate<CachedBlockPosition> predicate, KeywordArgs kwArgs) throws CommandSyntaxException {
    return setBlocksFromKeywordArgs(RegionArgumentType.getRegion(context, "region"), BlockFunctionArgumentType.getBlockFunction(context, "block"), context.getSource(), predicate, kwArgs);
  }

  public static final SimpleCommandExceptionType UNLOADED_POS = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.commands.fill.rejected", "unloaded=" + UnloadedPosBehavior.FORCE.asString()));

  public static int setBlocksWithDefaultKeywordArgs(Region region, BlockFunction blockFunction, ServerCommandSource source, @Nullable Predicate<CachedBlockPosition> predicate) throws CommandSyntaxException {
    return setBlocksInRegion(region, blockFunction, source, predicate, false, false, Block.NOTIFY_LISTENERS, 0, UnloadedPosBehavior.REJECT);
  }

  public static int setBlocksFromKeywordArgs(Region region, BlockFunction blockFunction, ServerCommandSource source, @Nullable Predicate<CachedBlockPosition> predicate, KeywordArgs kwArgs) throws CommandSyntaxException {
    return setBlocksInRegion(region, blockFunction, source, predicate, kwArgs.getBoolean("immediately"), kwArgs.getBoolean("bypass_limit"), getFlags(kwArgs), getModFlags(kwArgs), kwArgs.getArg("unloaded_pos"));
  }

  public static int setBlocksInRegion(Region region, BlockFunction blockFunction, ServerCommandSource source, @Nullable Predicate<CachedBlockPosition> predicate, boolean immediately, boolean bypassLimit, int flags, int modFlags, UnloadedPosBehavior unloadedPosBehavior) throws CommandSyntaxException {
    if (!bypassLimit && region.numberOfBlocksAffected() > REGION_SIZE_LIMIT) {
      throw REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), REGION_SIZE_LIMIT);
    }
    final ServerWorld world = source.getWorld();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BlockBox box = region.minContainingBlockBox();
      if (box != null && !LoadUtil.isPosLoaded(world, box)) {
        throw UNLOADED_POS.create();
      }
    }

    final Iterator<Void> mainIterator;
    final MutableInt numbersAffected = new MutableInt();
    final MutableBoolean hasUnloaded = new MutableBoolean();
    Stream<BlockPos> stream = region.stream();
    if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
      stream = stream.takeWhile(pos -> {
        final boolean chunkLoaded = world.isChunkLoaded(pos);
        if (!chunkLoaded) hasUnloaded.setTrue();
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      stream = stream.filter(pos -> {
        final boolean chunkLoaded = world.isChunkLoaded(pos);
        if (!chunkLoaded) hasUnloaded.setTrue();
        return chunkLoaded;
      });
    }

    if (predicate == null) {
      mainIterator = stream.<Void>map(blockPos -> {
            if (blockFunction.setBlock(world, blockPos, flags, modFlags)) {
              numbersAffected.increment();
            }
            return null;
          })
          .iterator();
    } else {
      List<BlockPos> posThatMatch = new ArrayList<>();
      Iterator<Void> testPosIterator = stream.<Void>map(blockPos -> {
            final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, true);
            if (predicate.test(cachedBlockPosition)) {
              posThatMatch.add(blockPos.toImmutable());
            }
            return null;
          })
          .iterator();
      Iterator<Void> placingIterator = posThatMatch.stream().<Void>map(blockPos -> {
            if (blockFunction.setBlock(world, blockPos, flags, modFlags)) {
              numbersAffected.increment();
            }
            return null;
          })
          .iterator();
      mainIterator = Iterators.concat(testPosIterator, placingIterator);
    }
    final Iterator<Void> finalClaimIterator = IterateUtils.singletonPeekingIterator(() -> CommandBridge.sendFeedback(source, () -> TextUtil.enhancedTranslatable(hasUnloaded.getValue() ? switch (unloadedPosBehavior) {
      case SKIP -> "enhancedCommands.commands.fill.complete_skipped";
      case BREAK -> "enhancedCommands.commands.fill.complete_broken";
      default -> "enhancedCommands.commands.fill.complete";
    } : "enhancedCommands.commands.fill.complete", numbersAffected.getValue()), true));
    final Iterator<Void> iterator = Iterators.concat(mainIterator, finalClaimIterator);

    if (!immediately && region.numberOfBlocksAffected() > 16384) {
      // The region is too large. Send a server task.
      ((ThreadExecutorExtension) source.getServer()).ec_addIteratorTask(Text.translatable("enhancedCommands.commands.fill.task_name", region.asString()), IterateUtils.batchAndSkip(iterator, 32768, 15));
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.fill.large_region", Long.toString(region.numberOfBlocksAffected())).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(iterator);
      return numbersAffected.intValue();
    }
  }

  public static int getFlags(@NotNull KeywordArgs args) {
    int value = 0;
    if (args.getBoolean("skip_light_update")) {
      value |= Block.SKIP_LIGHTING_UPDATES;
    }
    if (args.getBoolean("notify_listeners")) {
      value |= Block.NOTIFY_LISTENERS;
    }
    if (args.getBoolean("notify_neighbors")) {
      value |= Block.NOTIFY_NEIGHBORS;
    }
    if (args.getBoolean("force_state")) {
      value |= Block.FORCE_STATE;
    }
    if (args.getBoolean("force")) {
      value |= Block.FORCE_STATE;
      value &= ~Block.NOTIFY_NEIGHBORS;
    }
    return value;
  }

  public static int getModFlags(@NotNull KeywordArgs args) {
    int value = 0;
    if (args.getBoolean("post_process")) {
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
