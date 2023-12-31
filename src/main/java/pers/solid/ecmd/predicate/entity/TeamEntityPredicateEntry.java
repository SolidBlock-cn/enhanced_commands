package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.Styles;

public record TeamEntityPredicateEntry(String expectedTeamName, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    if (!(entity instanceof LivingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.team.not_living", displayName));
    } else {
      AbstractTeam abstractTeam = entity.getScoreboardTeam();
      String actualTeamName = abstractTeam == null ? "" : abstractTeam.getName();
      if (actualTeamName.equals(expectedTeamName)) {
        if (abstractTeam == null) {
          return TestResult.of(!hasNegation, Text.translatable("enhanced_commands.entity_predicate.team.true_nil", displayName));
        } else {
          return TestResult.of(!hasNegation, Text.translatable("enhanced_commands.entity_predicate.team.true", displayName, Text.literal(actualTeamName).styled(Styles.ACTUAL)));
        }
      } else {
        if (expectedTeamName.isEmpty()) {
          return TestResult.of(hasNegation, Text.translatable("enhanced_commands.entity_predicate.team.false_expect_nil", displayName, Text.literal(actualTeamName).styled(Styles.ACTUAL)));
        }
        final MutableText expectedText = Text.literal(expectedTeamName).styled(Styles.EXPECTED);
        if (abstractTeam == null) {
          return TestResult.of(hasNegation, Text.translatable("enhanced_commands.entity_predicate.team.false_nil", displayName, expectedText));
        } else {
          return TestResult.of(hasNegation, Text.translatable("enhanced_commands.entity_predicate.team.false", displayName, Text.literal(actualTeamName).styled(Styles.ACTUAL), expectedText));
        }
      }
    }
  }

  @Override
  public String toOptionEntry() {
    return "team=" + (hasNegation ? "!" : "") + expectedTeamName;
  }
}
