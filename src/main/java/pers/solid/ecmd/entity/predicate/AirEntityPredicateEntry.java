package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public record AirEntityPredicateEntry(BridgeIntRange air, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  public static final MapCodec<AirEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeIntRange.CODEC.fieldOf("air").forGetter(AirEntityPredicateEntry::air),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(AirEntityPredicateEntry::inverted)
  ).apply(i, AirEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    return air.test(entity.getAirSupply()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    return EntityPredicateEntry.testInt(entity, entity.getAirSupply(), air, Component.translatable("enhanced_commands.entity_predicate.air"), displayName, inverted);
  }

  @Override
  public String toOptionEntry() {
    return "air=" + (inverted ? "!" : "") + air.expressAsString();
  }

  @Override
  public EntityPredicateType<AirEntityPredicateEntry> getType() {
    return EntityPredicateTypes.AIR;
  }
}
