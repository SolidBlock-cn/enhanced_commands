package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.*;

import java.util.List;
import java.util.stream.Collectors;

public record ScoresEntityPredicateEntry(List<Entry> scores) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<ScoresEntityPredicateEntry> CODEC = Entry.CODEC.listOf().fieldOf("scores").xmap(ScoresEntityPredicateEntry::new, ScoresEntityPredicateEntry::scores);

  @Override
  public boolean test(@NotNull Entity entity) {
    final MinecraftServer server = entity.getServer();
    if (server == null) {
      return false;
    }
    final ServerScoreboard scoreboard = server.getScoreboard();
    for (var entry : scores) {
      ScoreboardObjective objective = scoreboard.getNullableObjective(entry.name());
      if (objective == null) {
        return false;
      }

      final ReadableScoreboardScore score = scoreboard.getScore(entity, objective);
      if (score == null) {
        return false;
      }

      int scoreValue = score.getScore();
      final boolean inverted = entry.inverted();
      final NumberRange.IntRange intRange = entry.score();
      final boolean test = intRange.test(scoreValue);
      if (test == inverted) return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    final MinecraftServer server = entity.getServer();
    if (server == null) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.score.not_on_server"));
    }
    boolean result = true;
    final ServerScoreboard scoreboard = server.getScoreboard();
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    for (var entry : scores) {
      ScoreboardObjective objective = scoreboard.getNullableObjective(entry.name());
      if (objective == null) {
        attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.score.no_objective", Text.literal(entry.name()).styled(Styles.TARGET))));
        result = false;
        continue;
      }

      final MutableText objectiveText = TextUtil.styled(objective.getDisplayName(), Styles.TARGET);
      final ReadableScoreboardScore score = scoreboard.getScore(entity, objective);
      if (score == null) {
        attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.score.no_player_score", displayName, objectiveText)));
        result = false;
        continue;
      }

      int scoreValue = score.getScore();
      final boolean inverted = entry.inverted();
      final NumberRange.IntRange intRange = entry.score();
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
  public @NotNull EntityPredicateType<ScoresEntityPredicateEntry> getType() {
    return EntityPredicateTypes.SCORE;
  }

  @Override
  public String toOptionEntry() {
    return scores.stream().map(Entry::asString).collect(Collectors.joining(", ", "scores={", "}"));
  }

  public record Entry(String name, NumberRange.IntRange score, boolean inverted) implements ExpressionConvertible {
    public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("name").forGetter(Entry::name),
        NumberRange.IntRange.CODEC.fieldOf("score").forGetter(Entry::score),
        Codec.BOOL.optionalFieldOf("inverted", false).forGetter(Entry::inverted)
    ).apply(i, Entry::new));

    @Override
    public @NotNull String asString() {
      return name + (inverted ? "=!" : "=") + StringUtil.wrapRange(score);
    }
  }
}
