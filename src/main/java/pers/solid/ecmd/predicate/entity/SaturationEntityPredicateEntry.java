package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;

public record SaturationEntityPredicateEntry(BridgeFloatRange floatRange, boolean inverted) implements EntityPredicateEntry {
  private static final Text CRITERION_NAME = Text.translatable("enhanced_commands.entity_predicate.saturation");

  @Override
  public boolean test(Entity entity) {
    return entity instanceof final PlayerEntity player && floatRange.test(player.getHungerManager().getSaturationLevel()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (!(entity instanceof final PlayerEntity player)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testFloat(player, player.getHungerManager().getSaturationLevel(), floatRange, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public String toOptionEntry() {
    return "saturation=" + (inverted ? "!" : "") + floatRange.asString();
  }
}
