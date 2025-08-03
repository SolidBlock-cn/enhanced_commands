package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;

import java.util.List;
import java.util.stream.Collectors;

public record AlternativesEntityPredicateEntry(List<EntityPredicate> predicates, boolean inverted) implements EntityPredicateEntry {
  public static final MapCodec<AlternativesEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EntityPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AlternativesEntityPredicateEntry::predicates),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(AlternativesEntityPredicateEntry::inverted)
  ).apply(i, AlternativesEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    return predicates.stream().anyMatch(entityPredicate -> entityPredicate.test(entity, context));
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    boolean result = false;
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    for (EntityPredicate entityPredicate : predicates) {
      final TestResult oneResult = entityPredicate.testAndDescribe(entity, context);
      attachments.add(oneResult);
      result |= oneResult.successes();
    }
    if (inverted) {
      if (result) {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.alternatives.fail_inverted", displayName), attachments.build());
      } else {
        return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.alternatives.pass_inverted", displayName), attachments.build());
      }
    } else {
      if (result) {
        return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.alternatives.pass", displayName), attachments.build());
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.alternatives.fail", displayName), attachments.build());
      }
    }
  }

  @Override
  public @NotNull EntityPredicateType<AlternativesEntityPredicateEntry> getType() {
    return EntityPredicateTypes.ALTERNATIVES;
  }

  @Override
  public @NotNull String toOptionEntry() {
    return "alternatives=" + (inverted ? "!" : "") + predicates.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ", "[", "]"));
  }
}
