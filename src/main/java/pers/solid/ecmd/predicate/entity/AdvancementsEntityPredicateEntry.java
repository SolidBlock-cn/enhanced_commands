package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.entity.Entity;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Map;
import java.util.stream.Collectors;

public record AdvancementsEntityPredicateEntry(@NotNull Map<@NotNull Identifier, @NotNull Either<@NotNull Map<@NotNull String, Boolean>, @NotNull Boolean>> map) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<AdvancementsEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.unboundedMap(Identifier.CODEC, Codec.either(Codec.unboundedMap(Codec.STRING, Codec.BOOL), Codec.BOOL)).fieldOf("advancements").forGetter(AdvancementsEntityPredicateEntry::map)
  ).apply(i, AdvancementsEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    if (!(entity instanceof ServerPlayerEntity serverPlayerEntity)) {
      return false;
    } else {
      PlayerAdvancementTracker advancementTracker = serverPlayerEntity.getAdvancementTracker();
      ServerAdvancementLoader advancementLoader = serverPlayerEntity.getServer().getAdvancementLoader();
      for (final var entry : map.entrySet()) {
        final Identifier advancementId = entry.getKey();
        final var value = entry.getValue();

        final AdvancementEntry advancementEntry = advancementLoader.get(advancementId);
        if (advancementEntry == null) {
          return false;
        }
        final AdvancementProgress progress = advancementTracker.getProgress(advancementEntry);

        if (value.left().isPresent()) {
          final Map<String, Boolean> expectedProgress = value.left().get();

          for (var progressEntry : expectedProgress.entrySet()) {
            final String criterionName = progressEntry.getKey();
            final CriterionProgress criterionProgress = progress.getCriterionProgress(criterionName);
            if (criterionProgress == null) {
              // the criterion does not exist -> false
              return false;
            }
            final boolean expectedValue = progressEntry.getValue();
            final boolean actualValue = criterionProgress.isObtained();
            if (expectedValue != actualValue) {
              return false;
            }
          }
        }
        if (value.right().isPresent()) {
          final boolean expectedValue = value.right().get();
          final boolean actualValue = progress.isDone();
          if (expectedValue != actualValue) {
            return false;
          }
        }
      }

    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    if (!(entity instanceof final ServerPlayerEntity player)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.not_player", displayName));
    }
    PlayerAdvancementTracker advancementTracker = player.getAdvancementTracker();
    ServerAdvancementLoader advancementLoader = player.getServer().getAdvancementLoader();

    boolean result = true;
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();

    for (final var entry : map.entrySet()) {
      final Identifier advancementId = entry.getKey();
      final var value = entry.getValue();

      final AdvancementEntry advancementEntry = advancementLoader.get(advancementId);
      if (advancementEntry == null) {
        // the advancement does not exist -> false
        attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.no_advancement", TextUtil.literal(advancementId).styled(Styles.TARGET))));
        result = false;
        continue;
      }
      final AdvancementProgress progress = advancementTracker.getProgress(advancementEntry);

      final MutableText advancementText = TextUtil.styled(advancementEntry.value().name().orElse(TextUtil.literal(advancementId)), Styles.TARGET);
      if (value.left().isPresent()) {
        boolean progressResult = true;
        final ImmutableList.Builder<TestResult> progressAttachments = new ImmutableList.Builder<>();
        final Map<String, Boolean> expectedProgress = value.left().get();

        for (var progressEntry : expectedProgress.entrySet()) {
          final String criterionName = progressEntry.getKey();
          final CriterionProgress criterionProgress = progress.getCriterionProgress(criterionName);
          final MutableText criterionText = Text.literal(criterionName).styled(Styles.TARGET);
          if (criterionProgress == null) {
            // the criterion does not exist -> false
            progressResult = false;
            progressAttachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.criterion.no_criterion", TextUtil.styled(advancementText, Styles.EXPECTED), criterionText)));
            continue;
          }
          final boolean expectedValue = progressEntry.getValue();
          final boolean actualValue = criterionProgress.isObtained();
          if (expectedValue == actualValue) {
            if (actualValue) {
              progressAttachments.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.advancements.criterion.completed_expected", displayName, criterionText)));
            } else {
              progressAttachments.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.advancements.criterion.not_completed_expected", displayName, criterionText)));
            }
          } else {
            if (actualValue) {
              progressAttachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.criterion.completed_unexpected", displayName, criterionText)));
            } else {
              progressAttachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.criterion.not_completed_unexpected", displayName, criterionText)));
            }
            progressResult = false;
          }
        }

        if (progressResult) {
          attachments.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.advancements.pass_with_criteria", displayName, advancementText), progressAttachments.build()));
        } else {
          attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.fail_with_criteria", displayName, advancementText), progressAttachments.build()));
          result = false;
        }
      }
      if (value.right().isPresent()) {
        final boolean expectedValue = value.right().get();
        final boolean actualValue = progress.isDone();
        if (expectedValue == actualValue) {
          if (actualValue) {
            attachments.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.advancements.completed_expected", displayName, advancementText)));
          } else {
            attachments.add(TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.advancements.not_completed_expected", displayName, advancementText)));
          }
        } else {
          if (actualValue) {
            attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.completed_unexpected", displayName, advancementText)));
          } else {
            attachments.add(TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.not_completed_unexpected", displayName, advancementText)));
          }
          result = false;
        }
      }
    }

    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.advancements.pass", displayName), attachments.build());
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.advancements.fail", displayName), attachments.build());
    }
  }

  @Override
  public @NotNull String toOptionEntry() {
    return "advancements=" + map.entrySet().stream().map(entry -> entry.getKey()
        + "=" + entry.getValue().map(
        criterionMap -> criterionMap.entrySet().stream().map(criterionEntry -> StringArgumentType.escapeIfRequired(criterionEntry.getKey()) + "=" + criterionEntry.getValue()).collect(Collectors.joining(", ", "{", "}")),
        String::valueOf
    )).collect(Collectors.joining(", ", "{", "}"));
  }

  @Override
  public @NotNull EntityPredicateType<AdvancementsEntityPredicateEntry> getType() {
    return EntityPredicateTypes.ADVANCEMENT;
  }
}
