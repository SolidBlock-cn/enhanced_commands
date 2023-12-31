package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.commons.lang3.tuple.Triple;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ScoreEntityPredicateEntry(Map<String, NumberRange.IntRange> expectedScore, List<Pair<String, NumberRange.IntRange>> invertedScores) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    boolean result = true;
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    final ServerScoreboard scoreboard = entity.getServer().getScoreboard();
    String entityName = entity.getEntityName();
    for (var triple : Iterables.concat(
        Iterables.transform(expectedScore.entrySet(), entry -> Triple.of(entry.getKey(), entry.getValue(), false)),
        Iterables.transform(invertedScores, input -> Triple.of(input.left(), input.right(), true))
    )) {
      ScoreboardObjective objective = scoreboard.getNullableObjective(triple.getLeft());
      if (objective == null) {
        attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.score.no_objective", Text.literal(triple.getLeft()).styled(Styles.TARGET))));
        result = false;
        continue;
      }

      final MutableText objectiveText = TextUtil.styled(objective.getDisplayName(), Styles.TARGET);
      if (!scoreboard.playerHasObjective(entityName, objective)) {
        attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.score.no_player_score", displayName, objectiveText)));
        result = false;
        continue;
      }

      ScoreboardPlayerScore score = scoreboard.getPlayerScore(entityName, objective);
      int scoreValue = score.getScore();
      final boolean inverted = triple.getRight();
      final NumberRange.IntRange intRange = triple.getMiddle();
      final boolean test = intRange.test(scoreValue);
      final MutableText actualValueText = TextUtil.literal(scoreValue).styled(Styles.ACTUAL);
      final MutableText expectedRangeText = Text.literal(StringUtil.wrapRange(intRange)).styled(Styles.EXPECTED);
      if (test) {
        attachments.add(TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.score.entry.in_range", displayName, objectiveText, actualValueText, expectedRangeText)));
      } else {
        attachments.add(TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.score.entry.out_of_range", displayName, objectiveText, actualValueText, expectedRangeText)));
      }
      result &= (test != inverted);
    }
    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.score.pass", displayName), attachments.build());
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.score.fail", displayName), attachments.build());
    }
  }

  @Override
  public String toOptionEntry() {
    return "scores={" + Stream.concat(
        expectedScore.entrySet().stream().map(entry -> entry.getKey() + "=" + StringUtil.wrapRange(entry.getValue())),
        invertedScores.stream().map(pair -> pair.left() + "=!" + StringUtil.wrapRange(pair.right()))
    ).collect(Collectors.joining(", ")) + "}";
  }
}
