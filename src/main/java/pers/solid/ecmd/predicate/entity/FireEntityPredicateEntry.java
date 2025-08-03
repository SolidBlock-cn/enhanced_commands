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

public record FireEntityPredicateEntry(BridgeIntRange time, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final Text CRITERION_NAME = Text.translatable("enhanced_commands.entity_predicate.fire");
  public static final MapCodec<FireEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeIntRange.CODEC.fieldOf("time").forGetter(FireEntityPredicateEntry::time),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(FireEntityPredicateEntry::inverted)
  ).apply(i, FireEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return time.test(entity.getFireTicks()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    return EntityPredicateEntry.testInt(entity, entity.getFireTicks(), time, CRITERION_NAME, displayName, inverted);
  }

  @Override
  public @NotNull EntityPredicateType<FireEntityPredicateEntry> getType() {
    return EntityPredicateTypes.FIRE;
  }

  @Override
  public @NotNull String toOptionEntry() {
    return "fire=" + (inverted ? "!" : "") + time.asString();
  }
}
