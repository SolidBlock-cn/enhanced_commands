package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record HealthMaxEntityPredicateEntry(boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<HealthMaxEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(HealthMaxEntityPredicateEntry::inverted)
  ).apply(i, HealthMaxEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity instanceof LivingEntity livingEntity && (livingEntity.getHealth() == livingEntity.getMaxHealth()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
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
  public @NotNull EntityPredicateType<HealthMaxEntityPredicateEntry> getType() {
    return EntityPredicateTypes.HEALTH_MAX;
  }

  @Override
  public String toOptionEntry() {
    return "health=" + (inverted ? "!" : "") + "max";
  }
}
