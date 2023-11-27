package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;

import java.util.Collection;

public record AlternativesEntityPredicateEntry(Collection<EntitySelector> entitySelectors, ServerCommandSource serverCommandSource, boolean inverted) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    boolean result = false;
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    for (EntitySelector entitySelector : entitySelectors) {
      final SelectorEntityPredicate selectorEntityPredicate = new SelectorEntityPredicate(entitySelector, serverCommandSource);
      final TestResult oneResult = selectorEntityPredicate.testAndDescribe(entity);
      attachments.add(oneResult);
      result |= oneResult.successes();
    }
    if (inverted) {
      if (result) {
        return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.alternatives.fail_inverted", displayName), attachments.build());
      } else {
        return TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.alternatives.pass_inverted", displayName), attachments.build());
      }
    } else {
      if (result) {
        return TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.alternatives.pass", displayName), attachments.build());
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.alternatives.fail", displayName), attachments.build());
      }
    }
  }

  @Override
  public String toOptionEntry() {
    return "alternatives=" + (inverted ? "!" : "") + "[...]";
  }
}
