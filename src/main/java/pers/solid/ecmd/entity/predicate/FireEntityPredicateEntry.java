package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

public record FireEntityPredicateEntry(BridgeIntRange time, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final Component CRITERION_NAME = Component.translatable("enhanced_commands.entity_predicate.fire");
  public static final MapCodec<FireEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeIntRange.CODEC.fieldOf("time").forGetter(FireEntityPredicateEntry::time),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(FireEntityPredicateEntry::inverted)
  ).apply(i, FireEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    return time.test(entity.getRemainingFireTicks()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    return EntityPredicateEntry.testInt(entity, entity.getRemainingFireTicks(), time, CRITERION_NAME, displayName, inverted);
  }

  @Override
  public EntityPredicateType<FireEntityPredicateEntry> getType() {
    return EntityPredicateTypes.FIRE;
  }

  @Override
  public String toOptionEntry() {
    return "fire=" + (inverted ? "!" : "") + time.asString();
  }
}
