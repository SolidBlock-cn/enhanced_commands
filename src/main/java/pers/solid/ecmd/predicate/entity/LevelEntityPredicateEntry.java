package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

public record LevelEntityPredicateEntry(BridgeIntRange intRange, boolean inverted) implements EntityPredicateEntry {
  private static final Text CRITERION_NAME = Text.translatable("enhanced_commands.entity_predicate.level");

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity instanceof PlayerEntity player)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testInt(player, player.experienceLevel, intRange, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public String toOptionEntry() {
    return "level=" + (inverted ? "!" : "") + intRange.asString();
  }
}
