package pers.solid.ecmd.command;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum SetBlocksCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.keywordArgsBuilder()
      .addOptionalArg("immediately", BoolArgumentType.bool(), false)
      .addOptionalArg("bypass_limit", BoolArgumentType.bool(), false)
      .addOptionalArg("skip_light_update", BoolArgumentType.bool(), false)
      .addOptionalArg("notify_listeners", BoolArgumentType.bool(), true)
      .addOptionalArg("notify_neighbors", BoolArgumentType.bool(), false)
      .addOptionalArg("force_state", BoolArgumentType.bool(), false)
      .builder();
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
    return execute(context, predicate, false, false, Block.NOTIFY_LISTENERS);
  }

  /**
   * Execute the command with the parameters read from args.
   */
  private static int execute(CommandContext<ServerCommandSource> context, Predicate<CachedBlockPosition> predicate, KeywordArgs kwArgs) throws CommandSyntaxException {
    final boolean immediately = kwArgs.getBoolean("immediately");
    final boolean bypassLimit = kwArgs.getBoolean("bypass_limit");
    return execute(context, predicate, immediately, bypassLimit, getFlags(kwArgs));
  }

  /**
   * Execute the command with the provided args.
   *
   * @param immediately Whether the operation is executed immediately, even if it is a large range.
   * @param bypassLimit If the range exceeds the limit, it will also perform it, instead of forbidding.
   * @param flags       The flags used to set block state.
   */
  private static int execute(CommandContext<ServerCommandSource> context, @Nullable Predicate<CachedBlockPosition> predicate, boolean immediately, boolean bypassLimit, int flags) throws CommandSyntaxException {
    final Region region = RegionArgumentType.getRegion(context, "region");
    if (!bypassLimit && region.numberOfBlocksAffected() > REGION_SIZE_LIMIT) {
      throw REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), REGION_SIZE_LIMIT);
    }
    final BlockFunction block = BlockFunctionArgumentType.getBlockFunction(context, "block");
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    // If the predicate exists, pre-determine positions that will be affected, instead of testing the predicate before placing each block.
    // To avoid that placing one block may affect the predicate tests thereafter.
    final Iterator<?> mainIterator;
    final MutableInt numbersAffected = new MutableInt();
    if (predicate == null) {
      mainIterator = region.stream()
          .peek(blockPos -> {
            if (block.setBlock(world, blockPos, flags))
              numbersAffected.increment();
          })
          .map(blockPos -> null)
          .iterator();
    } else {
      List<BlockPos> posThatMatch = new ArrayList<>();
      Iterator<?> testPosIterator = region.stream()
          .peek(blockPos -> {
            final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, true);
            if (predicate.test(cachedBlockPosition)) {
              posThatMatch.add(blockPos.toImmutable());
            }
          })
          .map(blockPos -> null)
          .iterator();
      IterateUtils.exhaust(testPosIterator);
      mainIterator = posThatMatch.stream()
          .peek(blockPos -> {
            if (block.setBlock(world, blockPos, 3))
              numbersAffected.increment();
          })
          .map(blockPos -> null)
          .iterator(); // Iterators.concat(testPosIterator, placingIterator);
    }
    final Iterator<?> iterator = Iterators.concat(mainIterator, IterateUtils.singletonPeekingIterator(() -> source.sendFeedback(Text.translatable("enhancedCommands.commands.setblocks.complete", numbersAffected.getValue()), true)));
    if (!immediately && region.numberOfBlocksAffected() > 16384) {
      // The region is too large. Send a server task.
      ((ThreadExecutorExtension) source.getServer()).ec_addIteratorTask(Text.translatable("enhancedCommands.commands.setblocks.task_name", region.asString()), IterateUtils.batchAndSkip(iterator, 8192, 7));
      source.sendFeedback(Text.translatable("enhancedCommands.commands.setblocks.large_region", region.numberOfBlocksAffected()).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(iterator);
      return numbersAffected.intValue();
    }
  }

  private static int getFlags(KeywordArgs args) {
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
}
