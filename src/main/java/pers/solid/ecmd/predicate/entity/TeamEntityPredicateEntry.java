package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

public record TeamEntityPredicateEntry(String team, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<TeamEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("team").forGetter(TeamEntityPredicateEntry::team),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TeamEntityPredicateEntry::inverted)
  ).apply(i, TeamEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    if (!(entity instanceof LivingEntity)) {
      return false;
    } else {
      AbstractTeam abstractTeam = entity.getScoreboardTeam();
      String string2 = abstractTeam == null ? "" : abstractTeam.getName();
      return string2.equals(team) != inverted;
    }
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    if (!(entity instanceof LivingEntity)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.team.not_living", displayName));
    } else {
      AbstractTeam abstractTeam = entity.getScoreboardTeam();
      String actualTeamName = abstractTeam == null ? "" : abstractTeam.getName();
      if (actualTeamName.equals(team)) {
        if (abstractTeam == null) {
          return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.team.true_nil", displayName));
        } else {
          return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.team.true", displayName, Text.literal(actualTeamName).styled(Styles.ACTUAL)));
        }
      } else {
        if (team.isEmpty()) {
          return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.team.false_expect_nil", displayName, Text.literal(actualTeamName).styled(Styles.ACTUAL)));
        }
        final MutableText expectedText = Text.literal(team).styled(Styles.EXPECTED);
        if (abstractTeam == null) {
          return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.team.false_nil", displayName, expectedText));
        } else {
          return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.team.false", displayName, Text.literal(actualTeamName).styled(Styles.ACTUAL), expectedText));
        }
      }
    }
  }

  @Override
  public @NotNull EntityPredicateType<TeamEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TEAM;
  }

  @Override
  public String toOptionEntry() {
    return "team=" + (inverted ? "!" : "") + team;
  }
}
