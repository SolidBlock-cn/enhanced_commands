package pers.solid.ecmd.command;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.region.Region;
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
import static net.minecraft.server.command.CommandManager.literal;

public enum SetBlocksCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final int POST_PROCESS_FLAG = 1;

  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.keywordArgsBuilder()
      .addOptionalArg("immediately", BoolArgumentType.bool(), false)
      .addOptionalArg("bypass_limit", BoolArgumentType.bool(), false)
      .addOptionalArg("skip_light_update", BoolArgumentType.bool(), false)
      .addOptionalArg("notify_listeners", BoolArgumentType.bool(), true)
      .addOptionalArg("notify_neighbors", BoolArgumentType.bool(), false)
      .addOptionalArg("force_state", BoolArgumentType.bool(), false)
      .addOptionalArg("post_process", BoolArgumentType.bool(), false)
      .addOptionalArg("unloaded_pos", new UnloadedPosBehaviorArgumentType(), UnloadedPosBehavior.REJECT)
      .build();
  public static final Dynamic2CommandExceptionType REGION_TOO_LARGE = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.commands.setblocks.region_too_large", a, b));
  public static final int REGION_SIZE_LIMIT = 16777215;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literal("setblocks")
        .then(argument("region", RegionArgumentType.region(registryAccess))
            .then(argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
                .executes(context -> execute(context, null))
                .then(argument("kwargs", KEYWORD_ARGS)
                    .executes(context -> execute(context, null, KeywordArgsArgumentType.getKeywordArgs("kwargs", context)))))));
    dispatcher.register(literal("replace")
        .then(argument("region", RegionArgumentType.region(registryAccess))
            .then(argument("predicate", BlockPredicateArgumentType.blockPredicate(registryAccess))
                .then(argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
                    .executes(context -> execute(context, cachedBlockPosition -> BlockPredicateArgumentType.getBlockPredicate(context, "predicate").test(cachedBlockPosition)))
                    .then(argument("kwargs", KEYWORD_ARGS)
                        .executes(context -> execute(context, cachedBlockPosition1 -> BlockPredicateArgumentType.getBlockPredicate(context, "predicate").test(cachedBlockPosition1), KeywordArgsArgumentType.getKeywordArgs("kwargs", context))))))));
  }

  /**
   * Execute the command with the default parameters.
   */
  private static int execute(CommandContext<ServerCommandSource> context, Predicate<CachedBlockPosition> predicate) throws CommandSyntaxException {
    return execute(context, predicate, false, false, Block.NOTIFY_LISTENERS, 0, UnloadedPosBehavior.REJECT);
  }

  /**
   * Execute the command with the parameters read from args.
   */
  private static int execute(CommandContext<ServerCommandSource> context, Predicate<CachedBlockPosition> predicate, KeywordArgs kwArgs) throws CommandSyntaxException {
    final boolean immediately = kwArgs.getBoolean("immediately");
    final boolean bypassLimit = kwArgs.getBoolean("bypass_limit");
    return execute(context, predicate, immediately, bypassLimit, getFlags(kwArgs), getModFlags(kwArgs), kwArgs.getArg("unloaded_pos"));
  }

  public static final SimpleCommandExceptionType UNLOADED_POS = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.commands.setblocks.rejected", "unloaded=" + UnloadedPosBehavior.FORCE.asString()));

  /**
   * Execute the command with the provided args.
   *
   * @param immediately Whether the operation is executed immediately, even if it is a large range.
   * @param bypassLimit If the range exceeds the limit, it will also perform it, instead of forbidding.
   * @param flags       The flags used to set block state.
   */
  private static int execute(CommandContext<ServerCommandSource> context, @Nullable Predicate<CachedBlockPosition> predicate, boolean immediately, boolean bypassLimit, int flags, int modFlags, UnloadedPosBehavior unloadedPosBehavior) throws CommandSyntaxException {
    final Region region = RegionArgumentType.getRegion(context, "region");
    if (!bypassLimit && region.numberOfBlocksAffected() > REGION_SIZE_LIMIT) {
      throw REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), REGION_SIZE_LIMIT);
    }
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BlockBox box = region.maxContainingBlockBox();
      if (box != null && (!world.isPosLoaded(box.getMinX(), box.getMinZ()) || !world.isPosLoaded(box.getMaxX(), box.getMaxZ()))) {
        throw UNLOADED_POS.create();
      }
    }
    final BlockFunction block = BlockFunctionArgumentType.getBlockFunction(context, "block");
    // If the predicate exists, pre-determine positions that will be affected, instead of testing the predicate before placing each block.
    // To avoid that placing one block may affect the predicate tests thereafter.
    final Iterator<?> mainIterator;
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
      mainIterator = stream
          .map(blockPos -> {
            if (block.setBlock(world, blockPos, flags, modFlags)) {
              numbersAffected.increment();
            }
            return null;
          })
          .iterator();
    } else {
      List<BlockPos> posThatMatch = new ArrayList<>();
      Iterator<?> testPosIterator = stream
          .<Void>map(blockPos -> {
            final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, true);
            if (predicate.test(cachedBlockPosition)) {
              posThatMatch.add(blockPos.toImmutable());
            }
            return null;
          })
          .iterator();
      IterateUtils.exhaust(testPosIterator);
      mainIterator = posThatMatch.stream()
          .<Void>map(blockPos -> {
            if (block.setBlock(world, blockPos, flags, modFlags)) {
              numbersAffected.increment();
            }
            return null;
          })
          .iterator(); // Iterators.concat(testPosIterator, placingIterator);
    }
    final Iterator<?> iterator = Iterators.concat(mainIterator, IterateUtils.singletonPeekingIterator(() -> CommandBridge.sendFeedback(source, () -> TextUtil.enhancedTranslatable(switch (unloadedPosBehavior) {
      case SKIP -> "enhancedCommands.commands.setblocks.complete_skipped";
      case BREAK -> "enhancedCommands.commands.setblocks.complete_broken";
      default -> "enhancedCommands.commands.setblocks.complete";
    }, numbersAffected.getValue()), true)));
    if (!immediately && region.numberOfBlocksAffected() > 16384) {
      // The region is too large. Send a server task.
      ((ThreadExecutorExtension) source.getServer()).ec_addIteratorTask(Text.translatable("enhancedCommands.commands.setblocks.task_name", region.asString()), IterateUtils.batchAndSkip(iterator, 32768, 15));
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhancedCommands.commands.setblocks.large_region", region.numberOfBlocksAffected()).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(iterator);
      return numbersAffected.intValue();
    }
  }

  public static int getFlags(KeywordArgs args) {
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
    return value;
  }

  public static int getModFlags(KeywordArgs args) {
    int value = 0;
    if (args.getBoolean("post_process")) {
      value |= POST_PROCESS_FLAG;
    }
    return value;
  }
}
