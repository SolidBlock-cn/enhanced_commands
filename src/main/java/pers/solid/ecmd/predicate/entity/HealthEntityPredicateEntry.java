package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;

public record HealthEntityPredicateEntry(BridgeFloatRange floatRange, boolean inverted) implements EntityPredicateEntry {
  private static final Text CRITERION_NAME = Text.translatable("enhanced_commands.entity_predicate.health");

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity instanceof LivingEntity livingEntity && (livingEntity.getHealth() == livingEntity.getMaxHealth()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity instanceof LivingEntity livingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.general.not_living_entity", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testFloat(livingEntity, livingEntity.getHealth(), floatRange, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public String toOptionEntry() {
    return "health=" + (inverted ? "!" : "") + floatRange.asString();
  }
}
