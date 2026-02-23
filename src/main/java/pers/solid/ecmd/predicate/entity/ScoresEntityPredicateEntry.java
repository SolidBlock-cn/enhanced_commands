package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
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
      Objective objective = scoreboard.getObjective(entry.name());
      if (objective == null) {
        return false;
      }

      final ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(entity, objective);
      if (score == null) {
        return false;
      }

      int scoreValue = score.value();
      final boolean inverted = entry.inverted();
      final MinMaxBounds.Ints intRange = entry.score();
      final boolean test = intRange.matches(scoreValue);
      if (test == inverted) return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) {
    final MinecraftServer server = entity.getServer();
    if (server == null) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.score.not_on_server"));
    }
    boolean result = true;
    final ServerScoreboard scoreboard = server.getScoreboard();
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    for (var entry : scores) {
      Objective objective = scoreboard.getObjective(entry.name());
      if (objective == null) {
        attachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.score.no_objective", Component.literal(entry.name()).withStyle(Styles.TARGET))));
        result = false;
        continue;
      }

      final MutableComponent objectiveText = TextUtil.styled(objective.getDisplayName(), Styles.TARGET);
      final ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(entity, objective);
      if (score == null) {
        attachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.score.no_player_score", displayName, objectiveText)));
        result = false;
        continue;
      }

      int scoreValue = score.value();
      final boolean inverted = entry.inverted();
      final MinMaxBounds.Ints intRange = entry.score();
      final boolean test = intRange.matches(scoreValue);
      final MutableComponent actualValueText = TextUtil.literal(scoreValue).withStyle(Styles.ACTUAL);
      final MutableComponent expectedRangeText = Component.literal(StringUtil.wrapRange(intRange)).withStyle(Styles.EXPECTED);
      if (test) {
        attachments.add(TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.score.entry.in_range", displayName, objectiveText, actualValueText, expectedRangeText)));
      } else {
        attachments.add(TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.score.entry.out_of_range", displayName, objectiveText, actualValueText, expectedRangeText)));
      }
      result &= (test != inverted);
    }
    if (result) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.score.pass", displayName), attachments.build());
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.score.fail", displayName), attachments.build());
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

  public record Entry(String name, MinMaxBounds.Ints score, boolean inverted) implements ExpressionConvertible {
    public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("name").forGetter(Entry::name),
        MinMaxBounds.Ints.CODEC.fieldOf("score").forGetter(Entry::score),
        Codec.BOOL.optionalFieldOf("inverted", false).forGetter(Entry::inverted)
    ).apply(i, Entry::new));

    @Override
    public @NotNull String asString() {
      return name + (inverted ? "=!" : "=") + StringUtil.wrapRange(score);
    }
  }
}
