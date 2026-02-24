package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.config.BlockOperationConfig;
import pers.solid.ecmd.mixins.ext.BlockableEventLoopExtension;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Iterator;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.argument.BlockPredicateArgument.blockPredicate;
import static pers.solid.ecmd.argument.BlockPredicateArgument.getBlockPredicate;
import static pers.solid.ecmd.argument.RegionArgument.region;

public enum TestForBlocksCommand implements TestForCommands.Entry {
  INSTANCE;
  public static final KeywordArgsArgument KEYWORD_ARGS = KeywordArgsArgument.builder()
      .addOptionalArg("immediately", BoolArgumentType.bool(), false)
      .addOptionalArg("bypass_limit", BoolArgumentType.bool(), false)
      .addOptionalArg("unloaded_pos", new UnloadedPosBehaviorArgument(), UnloadedPosBehavior.REJECT)
      .addOptionalArg("seed", LongArgumentType.longArg(), null)
      .build();

  @Override
  public void addArguments(LiteralArgumentBuilder<CommandSourceStack> testForBuilder, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    testForBuilder.then(literal("blocks")
        .then(argument("region", region(commandBuildContext))
            .then(argument("block_predicate", blockPredicate(commandBuildContext))
                .executes(context -> executeTestForBlocks(context, TestType.ANY, KEYWORD_ARGS.defaultArgs()))
                .then(argument("test_type", new SimpleEnumArgument<>(CommandEnumType.TEST_TYPE))
                    .executes(context -> executeTestForBlocks(context, KEYWORD_ARGS.defaultArgs()))
                    .then(argument("keyword_args", KEYWORD_ARGS)
                        .executes(TestForBlocksCommand::executeTestForBlocks))))));
  }

  private static int executeTestForBlocks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeTestForBlocks(context, SimpleEnumArgument.getEnumValue(context, "test_type"), KeywordArgsArgument.getKeywordArgs(context, "keyword_args"));
  }

  private static int executeTestForBlocks(CommandContext<CommandSourceStack> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    return executeTestForBlocks(context, SimpleEnumArgument.getEnumValue(context, "test_type"), keywordArgs);
  }

  private static int executeTestForBlocks(CommandContext<CommandSourceStack> context, TestType testType, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final boolean immediately = keywordArgs.getBoolean("immediately");
    final boolean bypassLimit = keywordArgs.getBoolean("bypass_limit");
    final UnloadedPosBehavior unloadedPosBehavior = keywordArgs.getArg("unloaded_pos");
    final Region region = RegionArgument.getRegion(context, "region");
    final @Nullable Long seed = keywordArgs.getArg("seed");

    final int regionSizeLimit = BlockOperationConfig.current.regionSizeLimit;
    if (!bypassLimit && region.numberOfBlocksAffected() > regionSizeLimit) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), regionSizeLimit);
    }
    final ServerLevel world = source.getLevel();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BoundingBox box = region.minContainingBlockBox();
      if (box != null && !LoadUtil.isPosLoaded(world, box)) {
        throw FillReplaceCommand.UNLOADED_POS.create();
      }
    }
    final BlockPredicate blockPredicate = getBlockPredicate(context, "block_predicate");

    MutableInt returnValue = new MutableInt();
    MutableInt blocksCounted = new MutableInt();
    MutableInt blocksMatched = new MutableInt();
    MutableInt blocksSkipped = unloadedPosBehavior == UnloadedPosBehavior.SKIP ? new MutableInt() : null;
    MutableBoolean shouldBreak = new MutableBoolean();

    final Iterable<Void> calculation = Iterables.transform(region.stream().takeWhile(i -> !shouldBreak.booleanValue())::iterator, blockPos -> {
      final BlockInWorld blockInWorld = new BlockInWorld(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
      if (blockInWorld.getState() == null) {
        if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
          blocksSkipped.increment();
          return null;
        } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.broken").withStyle(ChatFormatting.YELLOW), false);
          shouldBreak.setTrue();
          return null;
        }
      }
      blocksCounted.increment();
      final boolean test = blockPredicate.test(blockInWorld, new ExecutionContext(world.getRandom(), source, seed));
      if (test) blocksMatched.increment();

      if (testType == TestType.ANY && test) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.any.true", TextUtil.wrapVector(blockPos.immutable()), blockInWorld.getState().getBlock().getName()).withStyle(Styles.TRUE).append(" ").append(createToolbarText(blockPos, blockPredicate)), false);
        returnValue.setValue(0);
        shouldBreak.setTrue();
        return null;
      } else if (testType == TestType.ALL && !test) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.all.false", TextUtil.wrapVector(blockPos.immutable()), blockInWorld.getState().getBlock().getName()).withStyle(Styles.FALSE).append(" ").append(createToolbarText(blockPos, blockPredicate)), false);
        returnValue.setValue(1);
        shouldBreak.setTrue();
        return null;
      }
      return null;
    });

    final Iterable<Void> notifySkip = () -> IterateUtils.singletonPeekingIterator(() -> {
      if (unloadedPosBehavior == UnloadedPosBehavior.SKIP && blocksSkipped.intValue() > 0)
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.skipped", blocksSkipped.intValue()).enhanced$$().withStyle(ChatFormatting.YELLOW), false);
    });
    final Iterator<Void> conclusion = switch (testType) {
      case ANY -> IterateUtils.singletonPeekingIterator(() -> {
        if (blocksMatched.intValue() == 0) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.any.false", blocksCounted.intValue()).enhanced$$().withStyle(Styles.FALSE), false);
          returnValue.setValue(0);
        }
      });
      case ALL -> IterateUtils.singletonPeekingIterator(() -> {
        if (blocksMatched.intValue() == blocksCounted.intValue()) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.all.true", blocksCounted.intValue()).enhanced$$().withStyle(Styles.TRUE), false);
          returnValue.setValue(1);
        }
      });
      case COMPARE -> IterateUtils.singletonPeekingIterator(() -> {
        final int counted = blocksCounted.intValue();
        final int matched = blocksMatched.intValue();
        final int mismatched = counted - matched;

        if (matched >= mismatched) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.compare.true", counted, matched, mismatched).enhanced$$().withStyle(Styles.TRUE), false);
        } else {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.compare.false", counted, matched, mismatched).enhanced$$().withStyle(Styles.FALSE), false);
        }
        returnValue.setValue(matched >= mismatched ? 1 : 0);
      });
      case COUNT -> IterateUtils.singletonPeekingIterator(() -> {
        final int counted = blocksCounted.intValue();
        final int matched = blocksMatched.intValue();
        final int mismatched = counted - matched;
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.count.result", TextUtil.literal(counted).withStyle(Styles.RESULT), TextUtil.literal(matched).withStyle(Styles.RESULT), TextUtil.literal(mismatched).withStyle(Styles.RESULT)).enhanced$$(), false);
        returnValue.setValue(matched);
      });
      case PROPORTION -> IterateUtils.singletonPeekingIterator(() -> {
        final int counted = blocksCounted.intValue();
        final int matched = blocksMatched.intValue();
        final double proportion = 100d * matched / counted;
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.proportion.result", TextUtil.literal(counted).withStyle(Styles.RESULT), TextUtil.literal(matched).withStyle(Styles.RESULT), Component.literal(Double.isFinite(proportion) ? proportion + "%" : String.valueOf(proportion)).withStyle(Styles.RESULT)).enhanced$$(), false);
        returnValue.setValue(proportion * 100);
      });
    };

    final Iterable<Void> mainIterable = Iterables.concat(calculation, notifySkip, () -> conclusion);

    if (!immediately && region.numberOfBlocksAffected() > 16384) {
      final Component taskName = Component.translatable("enhanced_commands.commands.testfor.blocks.task_name", region.asString());
      ((BlockableEventLoopExtension) source.getServer()).addIteratorTask$ec(taskName, IterateUtils.batchAndSkip(mainIterable.iterator(), 32768, 3));
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.blocks.large_region", Long.toString(region.numberOfBlocksAffected())).enhanced$$().withStyle(ChatFormatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(mainIterable.iterator());
      return returnValue.shortValue();
    }
  }

  private static Component createToolbarText(BlockPos blockPos, BlockPredicate blockPredicate) {
    return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY).append(ComponentUtils.formatList(List.of(
        Component.translatable("enhanced_commands.commands.testfor.blocks.detail").withStyle(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/testfor block " + StringUtil.wrapVector(blockPos)))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.testfor_blocks.detail.description", TextUtil.wrapVector(blockPos))))),
        Component.translatable("enhanced_commands.commands.testfor.blocks.test").withStyle(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/testfor block " + StringUtil.wrapVector(blockPos) + " " + blockPredicate.asString()))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.testfor_blocks.test.description", TextUtil.wrapVector(blockPos))))),
        Component.translatable("enhanced_commands.commands.testfor.blocks.teleport").withStyle(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s " + StringUtil.wrapVector(blockPos)))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.testfor_blocks.teleport.description", TextUtil.wrapVector(blockPos))))),
        Component.translatable("enhanced_commands.commands.testfor.blocks.copy_pos").withStyle(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, StringUtil.wrapVector(blockPos.getCenter())))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.testfor_blocks.copy_pos.description", StringUtil.wrapVector(blockPos)))))
    ), Component.literal(" | "), text -> text.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.GRAY))).append("]");
  }

  public enum TestType implements StringRepresentable {
    ANY("any"),
    ALL("all"),
    COMPARE("compare"),
    COUNT("count"),
    PROPORTION("proportion");

    public static final StringIdentifiableCodec<TestType> CODEC = StringIdentifiableCodec.create(values());
    private final String name;

    TestType(String name) {
      this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
      return name;
    }
  }
}
