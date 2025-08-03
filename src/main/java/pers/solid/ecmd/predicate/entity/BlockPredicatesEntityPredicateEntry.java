package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.List;
import java.util.stream.Collectors;

public record BlockPredicatesEntityPredicateEntry(List<Pair<EnhancedPosArgument, BlockPredicate>> predicates) implements EntityPredicateEntry {
  public static final MapCodec<BlockPredicatesEntityPredicateEntry> CODEC = Codec.pair(EnhancedPosArgument.CODEC, BlockPredicate.CODEC).listOf().fieldOf("predicates").xmap(BlockPredicatesEntityPredicateEntry::new, BlockPredicatesEntityPredicateEntry::predicates);

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    for (Pair<EnhancedPosArgument, BlockPredicate> pair : predicates) {
      final var pos = pair.getFirst();
      final var predicate = pair.getSecond();
      if (!predicate.test(new CachedBlockPosition(entity.getWorld(), pos.toAbsoluteBlockPos(entity.getCommandSource()), false), new ExecutionContext(entity.getCommandSource()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    boolean result = true;
    for (Pair<EnhancedPosArgument, BlockPredicate> pair : predicates) {
      final var pos = pair.getFirst();
      final var predicate = pair.getSecond();
      final TestResult testResult = predicate.testAndDescribe(new CachedBlockPosition(entity.getWorld(), pos.toAbsoluteBlockPos(entity.getCommandSource()), false), new ExecutionContext(entity.getCommandSource()));
      attachments.add(testResult);
      result &= testResult.successes();
    }
    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.block.pass_multiple", displayName), attachments.build());
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.block.fail_multiple", displayName), attachments.build());
    }
  }

  @Override
  public @NotNull EntityPredicateType<BlockPredicatesEntityPredicateEntry> getType() {
    return EntityPredicateTypes.BLOCK_PREDICATES;
  }

  @Override
  public @NotNull String toOptionEntry() {
    return "block=" + predicates.stream().map(entry -> entry.getFirst().toString() + " = " + entry.getSecond().asString()).collect(Collectors.joining(", ", "{", "}"));
  }
}
