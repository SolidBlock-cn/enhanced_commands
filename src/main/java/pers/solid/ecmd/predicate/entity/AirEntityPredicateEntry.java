package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

public record AirEntityPredicateEntry(BridgeIntRange intRange, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<AirEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeIntRange.CODEC.fieldOf("int_range").forGetter(pers.solid.ecmd.predicate.entity.AirEntityPredicateEntry::intRange),
      Codec.BOOL.fieldOf("inverted").forGetter(pers.solid.ecmd.predicate.entity.AirEntityPredicateEntry::inverted)
  ).apply(i, AirEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return intRange.test(entity.getAir()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    return EntityPredicateEntry.testInt(entity, entity.getAir(), intRange, Text.translatable("enhanced_commands.entity_predicate.air"), displayName, inverted);
  }

  @Override
  public String toOptionEntry() {
    return "air=" + (inverted ? "!" : "") + intRange.asString();
  }

  @Override
  public @NotNull EntityPredicateType<AirEntityPredicateEntry> getType() {
    return EntityPredicateTypes.AIR;
  }
}
