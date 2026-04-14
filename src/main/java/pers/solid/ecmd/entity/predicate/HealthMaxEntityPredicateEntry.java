package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record HealthMaxEntityPredicateEntry(boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<HealthMaxEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(HealthMaxEntityPredicateEntry::inverted)
  ).apply(i, HealthMaxEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    return entity instanceof LivingEntity livingEntity && (livingEntity.getHealth() == livingEntity.getMaxHealth()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (!(entity instanceof LivingEntity livingEntity)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.commands.health.get.single.not_living", displayName));
    } else {
      final float actualHealth = livingEntity.getHealth();
      final MutableComponent actualHealthText = TextUtil.literal(actualHealth).withStyle(Styles.ACTUAL);
      final float maxHealth = livingEntity.getMaxHealth();
      final MutableComponent maxHealthText = TextUtil.literal(maxHealth);
      if (maxHealth == actualHealth) {
        return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.health.is_max", displayName, actualHealthText));
      } else {
        return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.health.is_not_max", displayName, actualHealthText, maxHealthText));
      }
    }
  }

  @Override
  public EntityPredicateType<HealthMaxEntityPredicateEntry> getType() {
    return EntityPredicateTypes.HEALTH_MAX;
  }

  @Override
  public String toOptionEntry() {
    return "health=" + (inverted ? "!" : "") + "max";
  }
}
