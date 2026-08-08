package pers.solid.ecmd.block.predicate;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;

import java.util.List;

public record AnyBlockPredicate(List<BlockPredicate> predicates) implements BlockPredicate {
  public static final MapCodec<AnyBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AnyBlockPredicate::predicates)).apply(i, AnyBlockPredicate::new));

  public AnyBlockPredicate(BlockPredicate... predicates) {
    this(List.of(predicates));
  }

  @Override
  public String expressAsString() {
    return "any(" + String.join(", ", Collections2.transform(predicates, ExpressionConvertible::expressAsString)) + ")";
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext context) {
    return predicates.stream().anyMatch(blockPredicate -> blockPredicate.test(blockInWorld, context));
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext context) {
    final ImmutableList.Builder<TestResult> results = new ImmutableList.Builder<>();
    int successes = 0;
    for (BlockPredicate blockPredicate : predicates) {
      TestResult testResult = blockPredicate.testAndDescribe(blockInWorld, context);
      results.add(testResult);
      if (testResult.successes())
        successes++;
    }
    final ImmutableList<TestResult> build = results.build();
    if (successes > 0) {
      return TestResult.of(true, Component.translatable("enhanced_commands.predicate.any.pass", successes, build.size()), build);
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.predicate.any.fail", successes, build.size()), build);
    }
  }

  @Override
  public BlockPredicateType<AnyBlockPredicate> getType() {
    return BlockPredicateTypes.ANY;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return predicates;
  }
}
