package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

public record AirMaxEntityPredicateEntry(boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    return (entity.getAir() == entity.getMaxAir()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final int actualAir = entity.getAir();
    final MutableText actualHealthText = TextUtil.literal(actualAir).styled(Styles.ACTUAL);
    final int maxAir = entity.getMaxAir();
    final MutableText maxHealthText = TextUtil.literal(maxAir);
    if (maxAir == actualAir) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.air.is_max", displayName, actualHealthText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.air.is_not_max", displayName, actualHealthText, maxHealthText));
    }
  }

  @Override
  public String toOptionEntry() {
    return "air=" + (inverted ? "!" : "") + "max";
  }
}
