package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.Map;
import java.util.stream.Collectors;

public record ScoreEntityPredicateEntry(Map<String, NumberRange.IntRange> expectedScore) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    boolean result = true;
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    final ServerScoreboard scoreboard = entity.getServer().getScoreboard();
    String entityName = entity.getEntityName();
    for (Map.Entry<String, NumberRange.IntRange> entry : expectedScore.entrySet()) {
      ScoreboardObjective objective = scoreboard.getNullableObjective(entry.getKey());
      if (objective == null) {
        attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.score.no_objective", Text.literal(entry.getKey()).styled(TextUtil.STYLE_FOR_TARGET))));
        result = false;
        continue;
      }

      final MutableText objectiveText = TextUtil.styled(objective.getDisplayName(), TextUtil.STYLE_FOR_TARGET);
      if (!scoreboard.playerHasObjective(entityName, objective)) {
        attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.score.no_player_score", displayName, objectiveText)));
        result = false;
        continue;
      }

      ScoreboardPlayerScore score = scoreboard.getPlayerScore(entityName, objective);
      int scoreValue = score.getScore();
      final boolean test = entry.getValue().test(scoreValue);
      final MutableText actualValueText = TextUtil.literal(scoreValue).styled(TextUtil.STYLE_FOR_ACTUAL);
      final MutableText expectedRangeText = Text.literal(StringUtil.wrapRange(entry.getValue())).styled(TextUtil.STYLE_FOR_EXPECTED);
      if (test) {
        attachments.add(TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.score.entry.pass", displayName, objectiveText, actualValueText, expectedRangeText)));
      } else {
        attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.score.entry.fail", displayName, objectiveText, actualValueText, expectedRangeText)));
        result = false;
      }
    }
    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.score.pass", displayName), attachments.build());
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.score.fail", displayName), attachments.build());
    }
  }

  @Override
  public String toOptionEntry() {
    return "scores={" + expectedScore.entrySet().stream().map(entry -> entry.getKey() + "=" + StringUtil.wrapRange(entry.getValue())).collect(Collectors.joining(", ")) + "}";
  }
}
