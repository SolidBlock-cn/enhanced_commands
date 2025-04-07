package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

public record AirEntityPredicateEntry(BridgeIntRange intRange, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    return intRange.test(entity.getAir()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    return EntityPredicateEntry.testInt(entity, entity.getAir(), intRange, Text.translatable("enhanced_commands.entity_predicate.air"), displayName, inverted);
  }

  @Override
  public @Nullable String toOptionEntry() {
    return "air=" + (inverted ? "!" : "") + intRange.asString();
  }
}
