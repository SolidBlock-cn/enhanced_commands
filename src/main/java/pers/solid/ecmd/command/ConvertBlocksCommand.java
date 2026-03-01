package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.ConstantBlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.extension.BlockableEventLoopExtension;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public enum ConvertBlocksCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgument keywordArgs = KeywordArgsArgument.builderFromShared(KeywordArgsCommon.CONVERT_BLOCKS, commandBuildContext)
        .addOptionalArg("affect_only", BlockPredicateArgument.blockPredicate(commandBuildContext), null)
        .addOptionalArg("immediately", BoolArgumentType.bool(), false)
        .addOptionalArg("bypass_limit", BoolArgumentType.bool(), false)
        .addOptionalArg("unloaded_pos", new UnloadedPosBehaviorArgument(), UnloadedPosBehavior.REJECT)
        .build();

    final IntFunction<Component> fallingBlockFeedback = value -> Component.translatable("enhanced_commands.commands.convertblocks.falling_block.complete", value).enhanced$$();
    final IntFunction<Component> blockDisplayFeedback = value -> Component.translatable("enhanced_commands.commands.convertblocks.block_display.complete", value).enhanced$$();
    ModCommands.registerWithRegionArgumentModification(
        dispatcher,
        ModCommands.literalR2("convertblocks"),
        ModCommands.literalR2("/convertblocks"),
        Commands.argument("region", RegionArgument.region(commandBuildContext))
            .then(Commands.literal("falling_block")
                .executes(context -> executeConvertBlocksToFallingBlock(ConvertBlockCommand::convertToFallingBlock, fallingBlockFeedback, keywordArgs.defaultArgs(), context))
                .then(Commands.argument("keyword_args", keywordArgs)
                    .executes(context -> executeConvertBlocksToFallingBlock(ConvertBlockCommand::convertToFallingBlock, fallingBlockFeedback, KeywordArgsArgument.getKeywordArgs(context, "keyword_args"), context))))
            .then(Commands.literal("block_display")
                .executes(context -> executeConvertBlocksToFallingBlock(ConvertBlockCommand::convertToBlockDisplay, blockDisplayFeedback, keywordArgs.defaultArgs(), context))
                .then(Commands.argument("keyword_args", keywordArgs)
                    .executes(context -> executeConvertBlocksToFallingBlock(ConvertBlockCommand::convertToBlockDisplay, blockDisplayFeedback, KeywordArgsArgument.getKeywordArgs(context, "keyword_args"), context))))
    );
  }

  public static int executeConvertBlocksToFallingBlock(ConvertBlockCommand.Conversion conversion, IntFunction<Component> feedback, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final Region region = RegionArgument.getRegion(context, "region");
    final CommandSourceStack source = context.getSource();
    boolean bypassLimit = keywordArgs.getBoolean("bypass_limit");
    UnloadedPosBehavior unloadedPosBehavior = keywordArgs.getArg("unloaded_pos");
    CompoundNbtFunction nbtFunction = keywordArgs.getArg("nbt");
    final @Nullable Long seed = keywordArgs.getArg("seed");
    if (!bypassLimit && region.numberOfBlocksAffected() > 16383) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), 16383);
    }
    final ServerLevel world = source.getLevel();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BoundingBox box = region.minContainingBlockBox();
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
        @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(pos);
        if (!chunkLoaded) hasUnloaded.setTrue();
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      stream = stream.filter(pos -> {
        @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(pos);
        if (!chunkLoaded) hasUnloaded.setTrue();
        return chunkLoaded;
      });
    }

    final BlockPredicate predicate = keywordArgs.getArg("affect_only");
    final int flags = FillReplaceCommand.getFlags(keywordArgs);
    final int modFlags = FillReplaceCommand.getModFlags(keywordArgs);
    final boolean affectFluid = keywordArgs.getBoolean("affect_fluid");
    final ExecutionContext executionContext = new ExecutionContext(world.getRandom(), source, seed);
    final Function<BlockPos, Void> mapper = blockPos -> {
      final Entity entity = conversion.getConvertedEntity(world, blockPos, flags, modFlags, affectFluid);
      if (nbtFunction != null) {
        try {
          entity.load(nbtFunction.apply(entity.saveWithoutId(new CompoundTag()), executionContext));
        } catch (CommandSyntaxException e) {
          throw new CommandRuntimeException(e);
        }
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
            return !blockState.isAir() && blockState != blockState.getFluidState().createLegacyBlock();
          });
        } else {
          stream = stream.filter(blockPos -> !world.getBlockState(blockPos).isAir());
        }
      }
      mainIterator = stream.map(mapper)
          .iterator();
    } else {
      LongList posThatMatch = new LongArrayList();
      final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
      Iterator<Void> testPosIterator = stream.<Void>map(blockPos -> {
            final BlockInWorld blockInWorld = new BlockInWorld(world, blockPos, true);
            if (predicate.test(blockInWorld, executionContext)) {
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
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.broken").withStyle(Styles.ACTUAL), false);
        } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.skipped").withStyle(Styles.ACTUAL), false);
        }
      }
      source.sendFeedback$ecBridge(() -> feedback.apply(numbersAffected.intValue()), true);
    });
    final Iterator<Void> iterator = Iterators.concat(mainIterator, finalClaimIterator);

    if (!keywordArgs.getBoolean("immediately") && region.numberOfBlocksAffected() > 2048) {
      ((BlockableEventLoopExtension) source.getServer()).addIteratorTask$ec(Component.translatable("enhanced_commands.commands.convertblocks.task_name", region.asString()), IterateUtils.batchAndSkip(iterator, 1024, 15));
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.large_region", Long.toString(region.numberOfBlocksAffected())).withStyle(ChatFormatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(iterator);
      return numbersAffected.intValue();
    }
  }
}
