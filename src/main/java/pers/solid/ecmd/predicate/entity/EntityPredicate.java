package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Predicate;

public interface EntityPredicate extends Predicate<Entity> {
  @Override
  boolean test(Entity entity);

  static TestResult successResult(Entity entity) {
    return TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.pass", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET)));
  }

  static TestResult failResult(Entity entity) {
    return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.fail", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET)));
  }

  static TestResult successOrFail(boolean successes, Entity entity) {
    return successes ? successResult(entity) : failResult(entity);
  }

  default TestResult testAndDescribe(Entity entity) throws CommandSyntaxException {
    final boolean test = test(entity);
    return successOrFail(test, entity);
  }
}
