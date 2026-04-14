package pers.solid.ecmd.entity.predicate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.List;

public record CollectorEntityPredicate(EntitySelectorCollector collector) implements SpecialEntityPredicate {
  public static final MapCodec<CollectorEntityPredicate> CODEC = EntitySelectorCollector.CODEC.fieldOf("collector").xmap(CollectorEntityPredicate::new, CollectorEntityPredicate::collector);
  private static final LoadingCache<@NotNull EntitySelectorCollector, LoadingCache<@NotNull Entity, List<? extends Entity>>> cache = CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(collector -> CacheBuilder.newBuilder().weakKeys().build(CacheLoader.from(entity -> collector.collectEntities(entity).toList()))));

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    final Entity sender = context.positionProvider.getEntity$ec();
    if (sender == null) {
      return false;
    }
    final List<? extends Entity> entities = cache.getUnchecked(collector).getUnchecked(sender);
    return entities.contains(entity);
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) throws CommandSyntaxException {
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
  public @NotNull EntityPredicateType<CollectorEntityPredicate> getType() {
    return EntityPredicateTypes.COLLECTOR;
  }

  @Override
  public @NotNull String asString() {
    return "@" + collector.getSerializedName();
  }
}
