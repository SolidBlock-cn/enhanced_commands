package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;

import java.util.function.Predicate;

public interface EntityPredicate extends Predicate<Entity> {
  @Override
  boolean test(Entity entity);

  static TestResult successResult(Entity entity) {
    return TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.pass", entity.getDisplayName()));
  }

  static TestResult failResult(Entity entity) {
    return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.fail", entity.getDisplayName()));
  }

  static TestResult successOrFail(boolean successes, Entity entity) {
    return successes ? successResult(entity) : failResult(entity);
  }

  default TestResult testAndDescribe(Entity entity) {
    final boolean test = test(entity);
    return successOrFail(test, entity);
  }
}
