package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Team;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public record TeamEntityPredicateEntry(String team, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  public static final MapCodec<TeamEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("team").forGetter(TeamEntityPredicateEntry::team),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TeamEntityPredicateEntry::inverted)
  ).apply(i, TeamEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    if (!(entity instanceof LivingEntity)) {
      return false;
    } else {
      Team abstractTeam = entity.getTeam();
      String string2 = abstractTeam == null ? "" : abstractTeam.getName();
      return string2.equals(team) != inverted;
    }
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (!(entity instanceof LivingEntity)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.team.not_living", displayName));
    } else {
      Team abstractTeam = entity.getTeam();
      String actualTeamName = abstractTeam == null ? "" : abstractTeam.getName();
      if (actualTeamName.equals(team)) {
        if (abstractTeam == null) {
          return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.team.true_nil", displayName));
        } else {
          return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.team.true", displayName, Component.literal(actualTeamName).withStyle(Styles.ACTUAL)));
        }
      } else {
        if (team.isEmpty()) {
          return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.team.false_expect_nil", displayName, Component.literal(actualTeamName).withStyle(Styles.ACTUAL)));
        }
        final MutableComponent expectedText = Component.literal(team).withStyle(Styles.EXPECTED);
        if (abstractTeam == null) {
          return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.team.false_nil", displayName, expectedText));
        } else {
          return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.team.false", displayName, Component.literal(actualTeamName).withStyle(Styles.ACTUAL), expectedText));
        }
      }
    }
  }

  @Override
  public EntityPredicateType<TeamEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TEAM;
  }

  @Override
  public String toOptionEntry() {
    return "team=" + (inverted ? "!" : "") + team;
  }
}
