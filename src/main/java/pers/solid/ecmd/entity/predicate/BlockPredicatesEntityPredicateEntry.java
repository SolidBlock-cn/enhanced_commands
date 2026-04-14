package pers.solid.ecmd.entity.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.TestResult;

import java.util.List;
import java.util.stream.Collectors;

public record BlockPredicatesEntityPredicateEntry(List<Pair<EnhancedCoordinates, BlockPredicate>> predicates) implements EntityPredicateEntry {
  private static final Codec<Pair<EnhancedCoordinates, BlockPredicate>> pairCodec = RecordCodecBuilder.create(instance -> instance.group(EnhancedCoordinates.CODEC.fieldOf("pos").forGetter(Pair::getFirst), BlockPredicate.CODEC.fieldOf("block").forGetter(Pair::getSecond)).apply(instance, Pair::of));
  public static final MapCodec<BlockPredicatesEntityPredicateEntry> CODEC = pairCodec.listOf().fieldOf("predicates").xmap(BlockPredicatesEntityPredicateEntry::new, BlockPredicatesEntityPredicateEntry::predicates);

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    for (Pair<EnhancedCoordinates, BlockPredicate> pair : predicates) {
      final var pos = pair.getFirst();
      final var predicate = pair.getSecond();
      if (!predicate.test(new BlockInWorld(entity.level(), pos.toAbsoluteBlockPos(PositionProvider.of(entity)), false), new ExecutionContext(PositionProvider.of(entity)))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) throws CommandSyntaxException {
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    boolean result = true;
    for (Pair<EnhancedCoordinates, BlockPredicate> pair : predicates) {
      final var pos = pair.getFirst();
      final var predicate = pair.getSecond();
      final TestResult testResult = predicate.testAndDescribe(new BlockInWorld(entity.level(), pos.toAbsoluteBlockPos(PositionProvider.of(entity)), false), new ExecutionContext(PositionProvider.of(entity)));
      attachments.add(testResult);
      result &= testResult.successes();
    }
    if (result) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.block.pass_multiple", displayName), attachments.build());
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.block.fail_multiple", displayName), attachments.build());
    }
  }

  @Override
  public @NotNull EntityPredicateType<BlockPredicatesEntityPredicateEntry> getType() {
    return EntityPredicateTypes.BLOCK_PREDICATES;
  }

  @Override
  public @NotNull String toOptionEntry() {
    return "block=" + predicates.stream().map(entry -> entry.getFirst().asString() + " = " + entry.getSecond().asString()).collect(Collectors.joining(", ", "{", "}"));
  }
}
