package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

public record HealthMaxEntityPredicateEntry(boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    return entity instanceof LivingEntity livingEntity && (livingEntity.getHealth() == livingEntity.getMaxHealth()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity instanceof LivingEntity livingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.commands.health.get.single.not_living", displayName));
    } else {
      final float actualHealth = livingEntity.getHealth();
      final MutableText actualHealthText = TextUtil.literal(actualHealth).styled(Styles.ACTUAL);
      final float maxHealth = livingEntity.getMaxHealth();
      final MutableText maxHealthText = TextUtil.literal(maxHealth);
      if (maxHealth == actualHealth) {
        return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.health.is_max", displayName, actualHealthText));
      } else {
        return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.health.is_not_max", displayName, actualHealthText, maxHealthText));
      }
    }
  }

  @Override
  public String toOptionEntry() {
    return "health=" + (inverted ? "!" : "") + "max";
  }
}
