package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.predicate.block.ConstantBlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.UnloadedPosBehavior;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public enum ConvertBlocksCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgs = KeywordArgsArgumentType.builder(ConvertBlockCommand.KEYWORD_ARGS)
        .addOptionalArg("affect_only", BlockPredicateArgumentType.blockPredicate(registryAccess), null)
        .addOptionalArg("immediately", BoolArgumentType.bool(), false)
        .addOptionalArg("bypass_limit", BoolArgumentType.bool(), false)
        .addOptionalArg("unloaded_pos", new UnloadedPosBehaviorArgumentType(), UnloadedPosBehavior.REJECT)
        .build();

    final IntFunction<Text> fallingBlockFeedback = value -> TextUtil.enhancedTranslatable("enhanced_commands.commands.convertblocks.falling_block.complete", value);
    final IntFunction<Text> blockDisplayFeedback = value -> TextUtil.enhancedTranslatable("enhanced_commands.commands.convertblocks.block_display.complete", value);
    ModCommands.registerWithRegionArgumentModification(
        dispatcher,
        ModCommands.literalR2("convertblocks"),
        ModCommands.literalR2("/convertblocks"),
        CommandManager.argument("region", RegionArgumentType.region(registryAccess))
            .then(CommandManager.literal("falling_block")
                .executes(context -> executeConvertBlocksToFallingBlock(ConvertBlockCommand::convertToFallingBlock, fallingBlockFeedback, keywordArgs.defaultArgs(), context))
                .then(CommandManager.argument("keyword_args", keywordArgs)
                    .executes(context -> executeConvertBlocksToFallingBlock(ConvertBlockCommand::convertToFallingBlock, fallingBlockFeedback, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))))
            .then(CommandManager.literal("block_display")
                .executes(context -> executeConvertBlocksToFallingBlock(ConvertBlockCommand::convertToBlockDisplay, blockDisplayFeedback, keywordArgs.defaultArgs(), context))
                .then(CommandManager.argument("keyword_args", keywordArgs)
                    .executes(context -> executeConvertBlocksToFallingBlock(ConvertBlockCommand::convertToBlockDisplay, blockDisplayFeedback, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))))
    );
  }

  public static int executeConvertBlocksToFallingBlock(ConvertBlockCommand.Conversion conversion, IntFunction<Text> feedback, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final Region region = RegionArgumentType.getRegion(context, "region");
    final ServerCommandSource source = context.getSource();
    boolean bypassLimit = keywordArgs.getBoolean("bypass_limit");
    UnloadedPosBehavior unloadedPosBehavior = keywordArgs.getArg("unloaded_pos");
    CompoundNbtFunction nbtFunction = keywordArgs.getArg("nbt");
    if (!bypassLimit && region.numberOfBlocksAffected() > 16383) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), 16383);
    }
    final ServerWorld world = source.getWorld();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BlockBox box = region.minContainingBlockBox();
      if (box != null && !LoadUtil.isPosLoaded(world, box)) {
        throw FillReplaceCommand.UNLOADED_POS.create();
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

    final BlockPredicateArgument blockPredicateArgument = keywordArgs.getArg("affect_only");
    final BlockPredicate predicate = blockPredicateArgument == null ? null : blockPredicateArgument.apply(source);
    final int flags = FillReplaceCommand.getFlags(keywordArgs);
    final int modFlags = FillReplaceCommand.getModFlags(keywordArgs);
    final boolean affectFluid = keywordArgs.getBoolean("affect_fluid");
    final Function<BlockPos, Void> mapper = blockPos -> {
      final Entity entity = conversion.getConvertedEntity(world, blockPos, flags, modFlags, affectFluid);
      if (nbtFunction != null) {
        entity.readNbt(nbtFunction.apply(entity.writeNbt(new NbtCompound())));
      }
      numbersAffected.increment();
      return null;
    };
    if (predicate == null || predicate instanceof ConstantBlockPredicate) {
      if (predicate == null) {
        if (affectFluid) {
          stream = stream.filter(blockPos -> {
            final BlockState blockState = world.getBlockState(blockPos);
            // 纯流体应该被过滤掉。
            return !blockState.isAir() && blockState != blockState.getFluidState().getBlockState();
          });
        } else {
          stream = stream.filter(blockPos -> !world.getBlockState(blockPos).isAir());
        }
      }
      mainIterator = stream.map(mapper)
          .iterator();
    } else {
      LongList posThatMatch = new LongArrayList();
      final BlockPos.Mutable mutable = new BlockPos.Mutable();
      Iterator<Void> testPosIterator = stream.<Void>map(blockPos -> {
            final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, true);
            if (predicate.test(cachedBlockPosition)) {
              posThatMatch.add(blockPos.asLong());
            }
            return null;
          })
          .iterator();
      Iterable<Void> placingIterator = () -> posThatMatch.longStream()
          .mapToObj(mutable::set)
          .map(mapper)
          .iterator();
      mainIterator = Iterables.concat(() -> testPosIterator, placingIterator).iterator();
    }

    final Iterator<Void> finalClaimIterator = IterateUtils.singletonPeekingIterator(() -> {
      if (hasUnloaded.booleanValue()) {
        if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
          CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.fill.broken").styled(TextUtil.STYLE_FOR_ACTUAL), false);
        } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
          CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.fill.skipped").styled(TextUtil.STYLE_FOR_ACTUAL), false);
        }
      }
      CommandBridge.sendFeedback(source, () -> feedback.apply(numbersAffected.intValue()), true);
    });
    final Iterator<Void> iterator = Iterators.concat(mainIterator, finalClaimIterator);

    if (!keywordArgs.getBoolean("immediately") && region.numberOfBlocksAffected() > 2048) {
      ((ThreadExecutorExtension) source.getServer()).ec_addIteratorTask(Text.translatable("enhanced_commands.commands.convertblocks.task_name", region.asString()), IterateUtils.batchAndSkip(iterator, 1024, 15));
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.fill.large_region", Long.toString(region.numberOfBlocksAffected())).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(iterator);
      return numbersAffected.intValue();
    }
  }
}
