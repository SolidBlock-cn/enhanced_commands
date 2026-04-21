package pers.solid.ecmd.entity.predicate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.List;

public record CollectorEntityPredicate(EntitySelectorCollector collector) implements SpecialEntityPredicate {
  public static final MapCodec<CollectorEntityPredicate> CODEC = EntitySelectorCollector.CODEC.fieldOf("collector").xmap(CollectorEntityPredicate::new, CollectorEntityPredicate::collector);
  private static final LoadingCache<EntitySelectorCollector, LoadingCache<Entity, List<? extends Entity>>> cache = CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(collector -> CacheBuilder.newBuilder().weakKeys().build(CacheLoader.from(entity -> collector.collectEntities(entity).toList()))));

  @Override
  public boolean test(Entity entity, ExecutionContext context) {
    final Entity sender = context.positionProvider.getEntity$ec();
    if (sender == null) {
      return false;
    }
    final List<? extends Entity> entities = cache.getUnchecked(collector).getUnchecked(sender);
    return entities.contains(entity);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final Entity sender = context.positionProvider.getEntity$ec();
    if (sender == null) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.collector.no_sender", displayName, collector.getDisplayName()));
    }
    final List<? extends Entity> entities = cache.getUnchecked(collector).getUnchecked(sender);
    if (entities.contains(entity)) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.collector.true", displayName, collector.getDisplayName(), sender.getFeedbackDisplayName()));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.collector.false", displayName, collector.getDisplayName(), sender.getFeedbackDisplayName()));
    }
  }

  @Override
  public EntityPredicateType<CollectorEntityPredicate> getType() {
    return EntityPredicateTypes.COLLECTOR;
  }

  @Override
  public String expressAsString() {
    return "@" + collector.getSerializedName();
  }
}
