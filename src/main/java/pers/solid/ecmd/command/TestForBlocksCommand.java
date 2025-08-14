package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Iterator;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.BlockPredicateArgumentType.blockPredicate;
import static pers.solid.ecmd.argument.BlockPredicateArgumentType.getBlockPredicate;
import static pers.solid.ecmd.argument.RegionArgumentType.region;

public enum TestForBlocksCommand implements TestForCommands.Entry {
  INSTANCE;
  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("immediately", BoolArgumentType.bool(), false)
      .addOptionalArg("bypass_limit", BoolArgumentType.bool(), false)
      .addOptionalArg("unloaded_pos", new UnloadedPosBehaviorArgumentType(), UnloadedPosBehavior.REJECT)
      .addOptionalArg("seed", LongArgumentType.longArg(), null)
      .build();

  @Override
  public void addArguments(LiteralArgumentBuilder<ServerCommandSource> testForBuilder, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    testForBuilder.then(literal("blocks")
        .then(argument("region", region(registryAccess))
            .then(argument("block_predicate", blockPredicate(registryAccess))
                .executes(context -> executeTestForBlocks(context, TestType.ANY, KEYWORD_ARGS.defaultArgs()))
                .then(argument("test_type", new SimpleEnumArgumentType<>(CommandEnumType.TEST_TYPE))
                    .executes(context -> executeTestForBlocks(context, KEYWORD_ARGS.defaultArgs()))
                    .then(argument("keyword_args", KEYWORD_ARGS)
                        .executes(TestForBlocksCommand::executeTestForBlocks))))));
  }

  private static int executeTestForBlocks(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeTestForBlocks(context, SimpleEnumArgumentType.getEnumValue(context, "test_type"), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"));
  }

  private static int executeTestForBlocks(CommandContext<ServerCommandSource> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    return executeTestForBlocks(context, SimpleEnumArgumentType.getEnumValue(context, "test_type"), keywordArgs);
  }

  private static int executeTestForBlocks(CommandContext<ServerCommandSource> context, TestType testType, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final boolean immediately = keywordArgs.getBoolean("immediately");
    final boolean bypassLimit = keywordArgs.getBoolean("bypass_limit");
    final UnloadedPosBehavior unloadedPosBehavior = keywordArgs.getArg("unloaded_pos");
    final Region region = RegionArgumentType.getRegion(context, "region");
    final @Nullable Long seed = keywordArgs.getArg("seed");

    if (!bypassLimit && region.numberOfBlocksAffected() > FillReplaceCommand.REGION_SIZE_LIMIT) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), FillReplaceCommand.REGION_SIZE_LIMIT);
    }
    final ServerWorld world = source.getWorld();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BlockBox box = region.minContainingBlockBox();
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
      final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
      if (cachedBlockPosition.getBlockState() == null) {
        if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
          blocksSkipped.increment();
          return null;
        } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
          source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.broken").formatted(Formatting.YELLOW), false);
          shouldBreak.setTrue();
          return null;
        }
      }
      blocksCounted.increment();
      final boolean test = blockPredicate.test(cachedBlockPosition, new ExecutionContext(world.getRandom(), source, seed));
      if (test) blocksMatched.increment();

      if (testType == TestType.ANY && test) {
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.any.true", TextUtil.wrapVector(blockPos.toImmutable()), cachedBlockPosition.getBlockState().getBlock().getName()).styled(Styles.TRUE).append(" ").append(createToolbarText(blockPos, blockPredicate)), false);
        returnValue.setValue(0);
        shouldBreak.setTrue();
        return null;
      } else if (testType == TestType.ALL && !test) {
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.all.false", TextUtil.wrapVector(blockPos.toImmutable()), cachedBlockPosition.getBlockState().getBlock().getName()).styled(Styles.FALSE).append(" ").append(createToolbarText(blockPos, blockPredicate)), false);
        returnValue.setValue(1);
        shouldBreak.setTrue();
        return null;
      }
      return null;
    });

    final Iterable<Void> notifySkip = () -> IterateUtils.singletonPeekingIterator(() -> {
      if (unloadedPosBehavior == UnloadedPosBehavior.SKIP && blocksSkipped.intValue() > 0)
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.skipped", blocksSkipped.intValue()).enhanced$$().formatted(Formatting.YELLOW), false);
    });
    final Iterator<Void> conclusion = switch (testType) {
      case ANY -> IterateUtils.singletonPeekingIterator(() -> {
        if (blocksMatched.intValue() == 0) {
          source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.any.false", blocksCounted.intValue()).enhanced$$().styled(Styles.FALSE), false);
          returnValue.setValue(0);
        }
      });
      case ALL -> IterateUtils.singletonPeekingIterator(() -> {
        if (blocksMatched.intValue() == blocksCounted.intValue()) {
          source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.all.true", blocksCounted.intValue()).enhanced$$().styled(Styles.TRUE), false);
          returnValue.setValue(1);
        }
      });
      case COMPARE -> IterateUtils.singletonPeekingIterator(() -> {
        final int counted = blocksCounted.intValue();
        final int matched = blocksMatched.intValue();
        final int mismatched = counted - matched;

        if (matched >= mismatched) {
          source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.compare.true", counted, matched, mismatched).enhanced$$().styled(Styles.TRUE), false);
        } else {
          source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.compare.false", counted, matched, mismatched).enhanced$$().styled(Styles.FALSE), false);
        }
        returnValue.setValue(matched >= mismatched ? 1 : 0);
      });
      case COUNT -> IterateUtils.singletonPeekingIterator(() -> {
        final int counted = blocksCounted.intValue();
        final int matched = blocksMatched.intValue();
        final int mismatched = counted - matched;
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.count.result", TextUtil.literal(counted).styled(Styles.RESULT), TextUtil.literal(matched).styled(Styles.RESULT), TextUtil.literal(mismatched).styled(Styles.RESULT)).enhanced$$(), false);
        returnValue.setValue(matched);
      });
      case PROPORTION -> IterateUtils.singletonPeekingIterator(() -> {
        final int counted = blocksCounted.intValue();
        final int matched = blocksMatched.intValue();
        final double proportion = 100d * matched / counted;
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.proportion.result", TextUtil.literal(counted).styled(Styles.RESULT), TextUtil.literal(matched).styled(Styles.RESULT), Text.literal(Double.isFinite(proportion) ? proportion + "%" : String.valueOf(proportion)).styled(Styles.RESULT)).enhanced$$(), false);
        returnValue.setValue(proportion * 100);
      });
    };

    final Iterable<Void> mainIterable = Iterables.concat(calculation, notifySkip, () -> conclusion);

    if (!immediately && region.numberOfBlocksAffected() > 16384) {
      final Text taskName = Text.translatable("enhanced_commands.commands.testfor.blocks.task_name", region.asString());
      ((ThreadExecutorExtension) source.getServer()).addIteratorTask$ec(taskName, IterateUtils.batchAndSkip(mainIterable.iterator(), 32768, 3));
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testfor.blocks.large_region", Long.toString(region.numberOfBlocksAffected())).enhanced$$().formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(mainIterable.iterator());
      return returnValue.shortValue();
    }
  }

  private static Text createToolbarText(BlockPos blockPos, BlockPredicate blockPredicate) {
    return Text.literal("[").formatted(Formatting.DARK_GRAY).append(Texts.join(List.of(
        Text.translatable("enhanced_commands.commands.testfor.blocks.detail").styled(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/testfor block " + StringUtil.wrapVector(blockPos)))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.testfor_blocks.detail.description", TextUtil.wrapVector(blockPos))))),
        Text.translatable("enhanced_commands.commands.testfor.blocks.test").styled(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/testfor block " + StringUtil.wrapVector(blockPos) + " " + blockPredicate.asString()))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.testfor_blocks.test.description", TextUtil.wrapVector(blockPos))))),
        Text.translatable("enhanced_commands.commands.testfor.blocks.teleport").styled(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s " + StringUtil.wrapVector(blockPos)))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.testfor_blocks.teleport.description", TextUtil.wrapVector(blockPos))))),
        Text.translatable("enhanced_commands.commands.testfor.blocks.copy_pos").styled(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, StringUtil.wrapVector(blockPos.toCenterPos())))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.testfor_blocks.copy_pos.description", StringUtil.wrapVector(blockPos)))))
    ), Text.literal(" | "), text -> text.formatted(Formatting.UNDERLINE, Formatting.GRAY))).append("]");
  }

  public enum TestType implements StringIdentifiable {
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
    public String asString() {
      return name;
    }
  }
}
