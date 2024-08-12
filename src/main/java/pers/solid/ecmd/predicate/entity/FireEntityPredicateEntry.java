package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

public record FireEntityPredicateEntry(BridgeIntRange intRange, boolean inverted) implements EntityPredicateEntry {
  public static final Text CRITERION_NAME = Text.translatable("enhanced_commands.entity_predicate.fire");

  @Override
  public boolean test(@NotNull Entity entity) {
    return intRange.test(entity.getFireTicks()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    return EntityPredicateEntry.testInt(entity, entity.getFireTicks(), intRange, CRITERION_NAME, displayName, inverted);
  }

  @Override
  public String toOptionEntry() {
    return "fire=" + (inverted ? "!" : "") + intRange.asString();
  }
}
