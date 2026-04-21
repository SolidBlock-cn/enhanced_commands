package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;

public record HealthEntityPredicateEntry(BridgeFloatRange health, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<HealthEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeFloatRange.CODEC.fieldOf("health").forGetter(HealthEntityPredicateEntry::health),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(HealthEntityPredicateEntry::inverted)
  ).apply(i, HealthEntityPredicateEntry::new));
  private static final Component CRITERION_NAME = Component.translatable("enhanced_commands.entity_predicate.health");

  @Override
  public boolean test(Entity entity) {
    return entity instanceof LivingEntity livingEntity && (livingEntity.getHealth() == livingEntity.getMaxHealth()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (!(entity instanceof LivingEntity livingEntity)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.general.not_living_entity", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testFloat(livingEntity, livingEntity.getHealth(), health, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public EntityPredicateType<HealthEntityPredicateEntry> getType() {
    return EntityPredicateTypes.HEALTH;
  }

  @Override
  public String toOptionEntry() {
    return "health=" + (inverted ? "!" : "") + health.expressAsString();
  }
}
