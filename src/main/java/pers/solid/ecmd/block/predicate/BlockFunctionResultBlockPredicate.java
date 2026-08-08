package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.mixins.accessor.BlockInWorldAccessor;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

import java.util.List;
import java.util.Objects;

public record BlockFunctionResultBlockPredicate(BlockFunction tryApply, BlockPredicate predicate) implements BlockPredicate {
  public static final MapCodec<BlockFunctionResultBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BlockFunction.CODEC.fieldOf("try_apply").forGetter(BlockFunctionResultBlockPredicate::tryApply),
      BlockPredicate.CODEC.fieldOf("predicate").forGetter(BlockFunctionResultBlockPredicate::predicate)
  ).apply(i, BlockFunctionResultBlockPredicate::new));

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext context) {
    final BlockState modifiedState;
    try {
      modifiedState = tryApply.getModifiedState(blockInWorld.getState(), blockInWorld.getState(), context.positionProvider.getWorld$ec(), blockInWorld.getPos(), new MutableObject<>(), context);
    } catch (CommandSyntaxException e) {
      return false;
    }

    final BlockInWorld newBlockInWorld = new BlockInWorld(blockInWorld.getLevel(), blockInWorld.getPos(), false);
    ((BlockInWorldAccessor) newBlockInWorld).setState(modifiedState);
    ((BlockInWorldAccessor) newBlockInWorld).setCachedEntity(true);
    return predicate.test(newBlockInWorld, context);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext context) {
    final BlockState modifiedState;
    try {
      modifiedState = tryApply.getModifiedState(blockInWorld.getState(), blockInWorld.getState(), context.positionProvider.getWorld$ec(), blockInWorld.getPos(), new MutableObject<>(), context);
    } catch (CommandSyntaxException e) {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.block_function_result.fail.error"), List.of(TestResult.of(false, ComponentUtils.fromMessage(e.getRawMessage()).copy())));
    }

    final BlockInWorld newBlockInWorld = new BlockInWorld(blockInWorld.getLevel(), blockInWorld.getPos(), false);
    ((BlockInWorldAccessor) newBlockInWorld).setState(modifiedState);
    ((BlockInWorldAccessor) newBlockInWorld).setCachedEntity(true);
    final TestResult testResult = predicate.testAndDescribe(newBlockInWorld, context);
    final MutableComponent stateText = modifiedState.getBlock().getName().withStyle(Styles.ACTUAL);
    if (testResult.successes()) {
      return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.block_function_result.pass", stateText), List.of(testResult));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.block_function_result.fail", stateText), List.of(testResult));
    }
  }

  @Override
  public BlockPredicateType<BlockFunctionResultBlockPredicate> getType() {
    return BlockPredicateTypes.TEST_BLOCK_FUNCTION_RESULT;
  }

  @Override
  public String expressAsString() {
    return "block-function-result(" + tryApply.expressAsString() + ", " + predicate.expressAsString() + ")";
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(tryApply);
  }

  public static class Parser implements FunctionContentParser.SequentialParams<BlockFunctionResultBlockPredicate> {
    private @Nullable BlockFunction tryApply;
    private @Nullable BlockPredicate predicate;

    @Override
    public @Nullable BlockFunctionResultBlockPredicate getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      Objects.requireNonNull(tryApply);
      Objects.requireNonNull(predicate);
      return new BlockFunctionResultBlockPredicate(tryApply, predicate);
    }

    @Override
    public int minSequentialParamsCount() {
      return 2;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> this.tryApply = BlockFunction.parse(parseContext);
        case 1 -> this.predicate = BlockPredicate.parse(parseContext);
      }
    }
  }
}
