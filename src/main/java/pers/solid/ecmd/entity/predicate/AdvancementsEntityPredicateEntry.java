package pers.solid.ecmd.entity.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.Map;
import java.util.stream.Collectors;

public record AdvancementsEntityPredicateEntry(Map<ResourceLocation, Either<Map<String, Boolean>, Boolean>> map) implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  public static final MapCodec<AdvancementsEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.unboundedMap(ResourceLocation.CODEC, Codec.either(Codec.unboundedMap(Codec.STRING, Codec.BOOL), Codec.BOOL)).fieldOf("advancements").forGetter(AdvancementsEntityPredicateEntry::map)
  ).apply(i, AdvancementsEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    if (!(entity instanceof ServerPlayer serverPlayerEntity)) {
      return false;
    } else {
      PlayerAdvancements advancementTracker = serverPlayerEntity.getAdvancements();
      ServerAdvancementManager advancementLoader = serverPlayerEntity.getServer().getAdvancements();
      for (final var entry : map.entrySet()) {
        final ResourceLocation advancementId = entry.getKey();
        final var value = entry.getValue();

        final AdvancementHolder advancementEntry = advancementLoader.get(advancementId);
        if (advancementEntry == null) {
          return false;
        }
        final AdvancementProgress progress = advancementTracker.getOrStartProgress(advancementEntry);

        if (value.left().isPresent()) {
          final Map<String, Boolean> expectedProgress = value.left().get();

          for (var progressEntry : expectedProgress.entrySet()) {
            final String criterionName = progressEntry.getKey();
            final CriterionProgress criterionProgress = progress.getCriterion(criterionName);
            if (criterionProgress == null) {
              // the criterion does not exist -> false
              return false;
            }
            final boolean expectedValue = progressEntry.getValue();
            final boolean actualValue = criterionProgress.isDone();
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
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (!(entity instanceof final ServerPlayer player)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.not_player", displayName));
    }
    PlayerAdvancements advancementTracker = player.getAdvancements();
    ServerAdvancementManager advancementLoader = player.getServer().getAdvancements();

    boolean result = true;
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();

    for (final var entry : map.entrySet()) {
      final ResourceLocation advancementId = entry.getKey();
      final var value = entry.getValue();

      final AdvancementHolder advancementEntry = advancementLoader.get(advancementId);
      if (advancementEntry == null) {
        // the advancement does not exist -> false
        attachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.no_advancement", TextUtil.literal(advancementId).withStyle(Styles.TARGET))));
        result = false;
        continue;
      }
      final AdvancementProgress progress = advancementTracker.getOrStartProgress(advancementEntry);

      final MutableComponent advancementText = TextUtil.styled(advancementEntry.value().name().orElse(TextUtil.literal(advancementId)), Styles.TARGET);
      if (value.left().isPresent()) {
        boolean progressResult = true;
        final ImmutableList.Builder<TestResult> progressAttachments = new ImmutableList.Builder<>();
        final Map<String, Boolean> expectedProgress = value.left().get();

        for (var progressEntry : expectedProgress.entrySet()) {
          final String criterionName = progressEntry.getKey();
          final CriterionProgress criterionProgress = progress.getCriterion(criterionName);
          final MutableComponent criterionText = Component.literal(criterionName).withStyle(Styles.TARGET);
          if (criterionProgress == null) {
            // the criterion does not exist -> false
            progressResult = false;
            progressAttachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.criterion.no_criterion", TextUtil.styled(advancementText, Styles.EXPECTED), criterionText)));
            continue;
          }
          final boolean expectedValue = progressEntry.getValue();
          final boolean actualValue = criterionProgress.isDone();
          if (expectedValue == actualValue) {
            if (actualValue) {
              progressAttachments.add(TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.advancements.criterion.completed_expected", displayName, criterionText)));
            } else {
              progressAttachments.add(TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.advancements.criterion.not_completed_expected", displayName, criterionText)));
            }
          } else {
            if (actualValue) {
              progressAttachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.criterion.completed_unexpected", displayName, criterionText)));
            } else {
              progressAttachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.criterion.not_completed_unexpected", displayName, criterionText)));
            }
            progressResult = false;
          }
        }

        if (progressResult) {
          attachments.add(TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.advancements.pass_with_criteria", displayName, advancementText), progressAttachments.build()));
        } else {
          attachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.fail_with_criteria", displayName, advancementText), progressAttachments.build()));
          result = false;
        }
      }
      if (value.right().isPresent()) {
        final boolean expectedValue = value.right().get();
        final boolean actualValue = progress.isDone();
        if (expectedValue == actualValue) {
          if (actualValue) {
            attachments.add(TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.advancements.completed_expected", displayName, advancementText)));
          } else {
            attachments.add(TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.advancements.not_completed_expected", displayName, advancementText)));
          }
        } else {
          if (actualValue) {
            attachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.completed_unexpected", displayName, advancementText)));
          } else {
            attachments.add(TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.not_completed_unexpected", displayName, advancementText)));
          }
          result = false;
        }
      }
    }

    if (result) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.advancements.pass", displayName), attachments.build());
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.advancements.fail", displayName), attachments.build());
    }
  }

  @Override
  public String toOptionEntry() {
    return "advancements=" + map.entrySet().stream().map(entry -> entry.getKey()
        + "=" + entry.getValue().map(
        criterionMap -> criterionMap.entrySet().stream().map(criterionEntry -> StringArgumentType.escapeIfRequired(criterionEntry.getKey()) + "=" + criterionEntry.getValue()).collect(Collectors.joining(", ", "{", "}")),
        String::valueOf
    )).collect(Collectors.joining(", ", "{", "}"));
  }

  @Override
  public EntityPredicateType<AdvancementsEntityPredicateEntry> getType() {
    return EntityPredicateTypes.ADVANCEMENT;
  }
}
