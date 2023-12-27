package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.fabricmc.fabric.mixin.command.EntitySelectorOptionsAccessor;
import net.minecraft.advancement.Advancement;
import net.minecraft.command.*;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.loot.LootGsons;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionManager;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityEffectPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixin.EntitySelectorOptionsMixin;
import pers.solid.ecmd.mixin.EntitySelectorReaderAccessor;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.EntitySelectorReaderExtension;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EntitySelectorOptionsExtension {
  /**
   * 此映射用于对 {@link net.minecraft.command.EntitySelectorOptions#getHandler(EntitySelectorReader, String, int)} 中遇到不要应用的选项时进行扩展，如果遇到了不可应用的选项，会尝试从此映射中查找不可应用的原因，而非简单的某选项不适用的消息。如果为 {@code null}，则不影响原版的行为。
   */
  public static final Map<String, InapplicableReasonProvider> INAPPLICABLE_REASONS = new HashMap<>();
  /**
   * 此映射用于选项名称的别称，当解析到不存在的选项名称时，会尝试解析到别称。
   */
  public static final Map<String, String> OPTION_NAME_ALIASES = new HashMap<>();


  public static final DynamicCommandExceptionType DUPLICATE_OPTION = new DynamicCommandExceptionType(optionName -> Text.translatable("enhanced_commands.argument.entity.options.duplicate_option", optionName));
  public static final DynamicCommandExceptionType DUPLICATE_OPTION_WITHOUT_INVERSION = new DynamicCommandExceptionType(optionName -> Text.translatable("enhanced_commands.argument.entity.options.duplicate_option_without_inversion", optionName));
  public static final DynamicCommandExceptionType MIXED_OPTION_INVERSION = new DynamicCommandExceptionType(optionName -> Text.translatable("enhanced_commands.argument.entity.options.mixed_option_inversion", optionName));
  public static final SimpleCommandExceptionType INVALID_LIMIT_FOR_AT_S = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_limit_for_@s"));
  public static final SimpleCommandExceptionType INVALID_SORT_FOR_AT_S = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_sort_for_@s"));
  public static final DynamicCommandExceptionType INVALID_TYPE_FOR_SELECTOR = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.entity.options.invalid_type_for_selector", o));
  public static final DynamicCommandExceptionType DISTANCE_ALREADY_EXPLICIT = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.entity.options.distance_already_explicit", o));
  public static final SimpleCommandExceptionType DISTANCE_ALREADY_IMPLICIT = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.distance_already_implicit"));
  public static final SimpleCommandExceptionType INVALID_NEGATIVE_LIMIT_WITH_SORTER = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_negative_limit_with_sorter"));
  public static final SimpleCommandExceptionType INVALID_SORTER_WITH_NEGATIVE_LIMIT = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_sorter_with_negative_limit"));

  private static void registerInapplicableReasons() {
    final var map = INAPPLICABLE_REASONS;
    markRequiringUniqueNoMixture("name");
    map.put("distance", (reader, option, restoreCursor) -> {
      reader.getReader().setCursor(restoreCursor);
      if (EntitySelectorReaderExtras.getOf(reader).implicitDistance) {
        return DISTANCE_ALREADY_IMPLICIT.createWithContext(reader.getReader());
      } else {
        return DUPLICATE_OPTION.createWithContext(reader.getReader(), option);
      }
    });
    markRequiringUniqueNoMixture("level");
    markRequiringUnique("x");
    markRequiringUnique("y");
    markRequiringUnique("z");
    markRequiringUnique("dx");
    markRequiringUnique("dy");
    markRequiringUnique("dz");
    markRequiringUnique("x_rotation");
    markRequiringUnique("y_rotation");
    map.put("limit", (reader, option, restoreCursor) -> {
      reader.getReader().setCursor(restoreCursor);
      return reader.isSenderOnly() ? INVALID_LIMIT_FOR_AT_S.createWithContext(reader.getReader()) : DUPLICATE_OPTION.createWithContext(reader.getReader(), "limit");
    });
    map.put("sort", (reader, option, restoreCursor) -> {
      final StringReader stringReader = reader.getReader();
      stringReader.setCursor(restoreCursor);
      if (EntitySelectorReaderExtras.getOf(reader).implicitNegativeLimit) {
        return INVALID_SORTER_WITH_NEGATIVE_LIMIT.createWithContext(stringReader);
      }
      return reader.isSenderOnly() ? INVALID_SORT_FOR_AT_S.createWithContext(stringReader) : DUPLICATE_OPTION.createWithContext(stringReader, "sort");
    });
    markRequiringUniqueNoMixture("gamemode");
    markRequiringUnique("team");
    map.put("type", (reader, option, restoreCursor) -> {
      if (reader.selectsEntityType()) {
        final EntitySelectorReaderExtras entitySelectorReaderExtras = EntitySelectorReaderExtras.getOf(reader);
        final String atVariable = entitySelectorReaderExtras.atVariable;
        final StringReader stringReader = reader.getReader();
        if (atVariable != null) {
          switch (atVariable) {
            case "a", "r", "p" -> {
              stringReader.setCursor(restoreCursor);
              return INVALID_TYPE_FOR_SELECTOR.createWithContext(stringReader, EntitySelectorReader.SELECTOR_PREFIX + atVariable);
            }
          }
        }
        stringReader.skipWhitespace();
        if (stringReader.canRead() && stringReader.read() == EntitySelectorReader.ARGUMENT_DEFINER && reader.readNegationCharacter()) {
          stringReader.setCursor(restoreCursor);
          return MIXED_OPTION_INVERSION.createWithContext(stringReader, option);
        } else {
          stringReader.setCursor(restoreCursor);
          return DUPLICATE_OPTION.createWithContext(stringReader, option);
        }
      }
      return null;
    });
    markRequiringUnique("scores");
    markRequiringUnique("advancements");
    final InapplicableReasonProvider providerForR = (reader, option, restoreCursor) -> {
      if (EntitySelectorReaderExtras.getOf(reader).implicitDistance) {
        reader.getReader().setCursor(restoreCursor);
        return DUPLICATE_OPTION.createWithContext(reader.getReader(), option);
      } else {
        reader.getReader().setCursor(restoreCursor);
        return DISTANCE_ALREADY_EXPLICIT.createWithContext(reader.getReader(), option);
      }
    };
    map.put("r", providerForR);
    map.put("rm", providerForR);
  }

  /**
   * 将选择名称表示为只允许出现一次。此方法不会实际进行限制，而是会在当选择因 inapplicable 而无法使用时，直接视为是因为重复使用了参数。
   */
  private static void markRequiringUnique(String optionName) {
    INAPPLICABLE_REASONS.put(optionName, (reader, option, restoreCursor) -> {
      reader.getReader().setCursor(restoreCursor);
      return DUPLICATE_OPTION.createWithContext(reader.getReader(), option);
    });
  }

  /**
   * 将选择器名称表示为此允许出现一次，以及不能用时使用反向和正向的用法。反向用法的判断标准为，在等号后面（跳过空格）是否存在感叹号。
   */
  private static void markRequiringUniqueNoMixture(String optionName) {
    INAPPLICABLE_REASONS.put(optionName, (reader, option, restoreCursor) -> {
      final StringReader stringReader = reader.getReader();
      stringReader.skipWhitespace();
      if (stringReader.canRead() && stringReader.read() == EntitySelectorReader.ARGUMENT_DEFINER) {
        stringReader.skipWhitespace();
        if (reader.readNegationCharacter()) {
          stringReader.setCursor(restoreCursor);
          return MIXED_OPTION_INVERSION.createWithContext(stringReader, option);
        }
      }
      stringReader.setCursor(restoreCursor);
      return DUPLICATE_OPTION_WITHOUT_INVERSION.createWithContext(stringReader, option);
    });
  }

  private static void registerOptionAliases() {
    final var map = OPTION_NAME_ALIASES;
    map.put("c", "limit");
    map.put("m", "gamemode");
  }

  private static void registerModOptions() {
    putOption("r", reader -> {
      final NumberRange.FloatRange original = reader.getDistance();
      final StringReader stringReader = reader.getReader();
      final int cursorBeforeValue = stringReader.getCursor();
      final float value = stringReader.readFloat();
      EntitySelectorReaderExtras.getOf(reader).implicitDistance = true;
      if (original.getMin() == null) {
        reader.setDistance(NumberRange.FloatRange.atMost(value));
      } else {
        if (value < original.getMin()) {
          final int cursorAfterValue = stringReader.getCursor();
          stringReader.setCursor(cursorBeforeValue);
          throw CommandSyntaxExceptionExtension.withCursorEnd(NumberRange.EXCEPTION_SWAPPED.createWithContext(stringReader), cursorAfterValue);
        } else if (value < 0) {
          final int cursorAfterValue = stringReader.getCursor();
          stringReader.setCursor(cursorBeforeValue);
          throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptions.NEGATIVE_DISTANCE_EXCEPTION.createWithContext(stringReader), cursorAfterValue);
        }
        reader.setDistance(NumberRange.FloatRange.between(original.getMin(), value));
      }
    }, reader -> reader.getDistance().isDummy() || EntitySelectorReaderExtras.getOf(reader).implicitDistance && reader.getDistance().getMax() != null, Text.translatable("enhanced_commands.argument.entity.options.r.description"));
    putOption("rm", reader -> {
      final NumberRange.FloatRange original = reader.getDistance();
      final StringReader stringReader = reader.getReader();
      final int cursorBeforeValue = stringReader.getCursor();
      final float value = stringReader.readFloat();
      EntitySelectorReaderExtras.getOf(reader).implicitDistance = true;
      if (original.getMax() == null) {
        reader.setDistance(NumberRange.FloatRange.atLeast(value));
      } else {
        if (value > original.getMax()) {
          final int cursorAfterValue = stringReader.getCursor();
          stringReader.setCursor(cursorBeforeValue);
          throw CommandSyntaxExceptionExtension.withCursorEnd(NumberRange.EXCEPTION_SWAPPED.createWithContext(stringReader), cursorAfterValue);
        } else if (value < 0) {
          final int cursorAfterValue = stringReader.getCursor();
          stringReader.setCursor(cursorBeforeValue);
          throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptions.NEGATIVE_DISTANCE_EXCEPTION.createWithContext(stringReader), cursorAfterValue);
        }
        reader.setDistance(NumberRange.FloatRange.between(value, original.getMax()));
      }
    }, reader -> reader.getDistance().isDummy() || EntitySelectorReaderExtras.getOf(reader).implicitDistance && reader.getDistance().getMax() != null, Text.translatable("enhanced_commands.argument.entity.options.rm.description"));

    putOption("region", reader -> {
      final SuggestedParser parser = new SuggestedParser(reader.getReader());
      reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> parser.buildSuggestions(EntitySelectorReaderExtras.getOf(reader).context, suggestionsBuilder));
      final CommandRegistryAccess registryAccess = MixinShared.getCommandRegistryAccess();
      final RegionArgument regionArgument = RegionArgument.parse(registryAccess, parser, false);

      EntitySelectorReaderExtras.getOf(reader).addFunction(source -> {
        final Region region;
        region = regionArgument.toAbsoluteRegion(source);
        return entity -> region.contains(entity.getPos());
      });
    }, Predicates.alwaysTrue(), Text.translatable("enhanced_commands.argument.entity.options.region"));

    putOption("alternatives", reader -> {
      final boolean inverted = reader.readNegationCharacter();
      final StringReader stringReader = reader.getReader();
      reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest("[").buildFuture());
      if (stringReader.canRead() && stringReader.peek() == '[') {
        stringReader.skip();
        stringReader.skipWhitespace();
      } else {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(stringReader, "[");
      }

      // 在左方括号的后面，开始解析实体谓词。
      ImmutableList.Builder<EntitySelector> entitySelectors = new ImmutableList.Builder<>();
      while (true) {
        stringReader.skipWhitespace();
        if (stringReader.canRead() && stringReader.peek() == ']') {
          stringReader.skip();
          break;
        }

        final EntitySelectorReader newReader = new EntitySelectorReader(stringReader);
        final int cursorBeforeRead = stringReader.getCursor();
        try {
          final EntitySelector entitySelector = EntitySelectors.readOmittibleEntitySelector(newReader);
          entitySelectors.add(entitySelector);
        } catch (CommandSyntaxException e) {
          reader.setSuggestionProvider((builder, consumer) -> newReader.listSuggestions(builder, suggestionsBuilder -> {
            consumer.accept(suggestionsBuilder);
            if (stringReader.getCursor() == cursorBeforeRead) {
              suggestionsBuilder.suggest("]");
            }
          }));
          throw e;
        }

        stringReader.skipWhitespace();
        if (stringReader.canRead()) {
          if (stringReader.peek() == ',') {
            stringReader.skip();
            continue;
          } else if (stringReader.peek() == ']') {
            stringReader.skip();
            break;
          }
        }

        // 如果读取到不完整的玩家名称，即使是没有出错的，也先暂缓调整建议。
        // 可能作为不玩家的玩家的一部分的名称。

        reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> {
          final SuggestionsBuilder prevSuggestionsBuilder = suggestionsBuilder.createOffset(cursorBeforeRead);
          suggestionsBuilderConsumer.accept(prevSuggestionsBuilder);
          final Suggestions prevSuggestions = prevSuggestionsBuilder.build();
          if (!prevSuggestions.isEmpty()) {
            return CompletableFuture.completedFuture(prevSuggestions);
          } else {
            return suggestionsBuilder.suggest(",").suggest("]").buildFuture();
          }
        });
        throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(stringReader, ",", "]");
      }

      final ImmutableList<EntitySelector> build = entitySelectors.build();
      EntitySelectorReaderExtras.getOf(reader).addFunction(source -> {
        final var predicate = Predicates.or(IterateUtils.transformFailableImmutableList(build, input -> SelectorEntityPredicate.asPredicate(input, source)));
        return inverted ? Predicates.not(predicate) : predicate;
      });
    }, Predicates.alwaysTrue(), Text.translatable("enhanced_commands.argument.entity.options.alternatives"));

    putOption("health", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.readNegationCharacter();
      checkNoInversionMix(reader, "health", inverted);
      final int cursorBefore = stringReader.getCursor();
      reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> ParsingUtil.suggestString("max", suggestionsBuilder).buildFuture());
      final String unquotedString = stringReader.readUnquotedString();
      if ("max".equals(unquotedString)) {
        reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new HealthMaxEntityPredicateEntry(inverted));
      } else {
        stringReader.setCursor(cursorBefore);
        final FloatRangeArgument floatRange = FloatRangeArgument.parse(stringReader, true);
        reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new HealthEntityPredicateEntry(floatRange, inverted));
      }
      markParamAsUsed(reader, "health", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "health"), Text.translatable("enhanced_commands.argument.entity.options.health"));
    markRequiringUniqueNoMixture("health");
    putOption("air", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.readNegationCharacter();
      checkNoInversionMix(reader, "air", inverted);
      final int cursorBefore = stringReader.getCursor();
      reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> ParsingUtil.suggestString("max", suggestionsBuilder).buildFuture());
      final String unquotedString = stringReader.readUnquotedString();
      if ("max".equals(unquotedString)) {
        reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new AirMaxEntityPredicateEntry(inverted));
      } else {
        stringReader.setCursor(cursorBefore);
        final NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
        reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new AirEntityPredicateEntry(intRange, inverted));
      }
      markParamAsUsed(reader, "air", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "air"), Text.translatable("enhanced_commands.argument.entity.options.air"));
    markRequiringUniqueNoMixture("air");
    putOption("food", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.readNegationCharacter();
      checkNoInversionMix(reader, "food", inverted);
      final NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
      reader.setIncludesNonPlayers(false);
      EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new FoodEntityPredicateEntry(intRange, inverted));
      markParamAsUsed(reader, "food", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "food"), Text.translatable("enhanced_commands.argument.entity.options.food"));
    markRequiringUniqueNoMixture("food");
    putOption("saturation", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.readNegationCharacter();
      checkNoInversionMix(reader, "saturation", inverted);
      final FloatRangeArgument floatRange = FloatRangeArgument.parse(stringReader, true);
      reader.setIncludesNonPlayers(false);
      EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new SaturationEntityPredicateEntry(floatRange, inverted));
      markParamAsUsed(reader, "saturation", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "saturation"), Text.translatable("enhanced_commands.argument.entity.options.saturation"));
    markRequiringUniqueNoMixture("saturation");
    putOption("exhaustion", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.readNegationCharacter();
      checkNoInversionMix(reader, "exhaustion", inverted);
      final FloatRangeArgument floatRange = FloatRangeArgument.parse(stringReader, true);
      reader.setIncludesNonPlayers(false);
      EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new ExhaustionEntityPredicateEntry(floatRange, inverted));
      markParamAsUsed(reader, "exhaustion", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "exhaustion"), Text.translatable("enhanced_commands.argument.entity.options.exhaustion"));
    markRequiringUniqueNoMixture("exhaustion");
    putOption("fire", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.readNegationCharacter();
      checkNoInversionMix(reader, "fire", inverted);
      final NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
      EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new FireEntityPredicateEntry(intRange, inverted));
      markParamAsUsed(reader, "fire", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "fire"), Text.translatable("enhanced_commands.argument.entity.options.fire"));
    markRequiringUniqueNoMixture("fire");

    putOption("pose", reader -> {
      final boolean inverted = reader.readNegationCharacter();
      checkNoInversionMix(reader, "pose", inverted);
      final int cursorBefore = reader.getReader().getCursor();
      reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> CommandSource.suggestMatching(PoseEntityPredicateEntry.ENTITY_POSE_NAMES.values(), suggestionsBuilder));
      final String s = reader.getReader().readUnquotedString();
      final EntityPose entityPose = PoseEntityPredicateEntry.ENTITY_POSE_NAMES.inverse().get(s);
      if (entityPose != null) {
        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new PoseEntityPredicateEntry(entityPose, inverted));
        markParamAsUsed(reader, "pose", inverted);
      } else {
        final int cursorAfter = reader.getReader().getCursor();
        reader.getReader().setCursor(cursorBefore);
        throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader.getReader()), cursorAfter);
      }
    }, reader -> isNeverPositivelyUsed(reader, "pose"), Text.translatable("enhanced_commands.argument.entity_predicate.pose"));
    markRequiringUniqueNoMixture("pose");

    // 以下对应 EntityFlags 部分

    putSimpleBooleanOption("on_fire", Entity::isOnFire);
    // 注意：这里的 isSneaking 于 EntityFlagsPredicate 所使用的 isInSneakingPose 不同
    putSimpleBooleanOption("sneaking", Entity::isSneaking);
    putSimpleBooleanOption("sprinting", Entity::isSprinting);
    putSimpleBooleanOption("swimming", Entity::isSwimming);
    putSimpleBooleanOption("baby", entity -> entity instanceof LivingEntity livingEntity && livingEntity.isBaby());

    // 检测实体所在的方块
    putOption("block", reader -> {
      final StringReader stringReader = reader.getReader();
      stringReader.skipWhitespace();
      final SuggestedParser parser = new SuggestedParser(stringReader);
      if (stringReader.canRead() && stringReader.peek() == '{') {
        stringReader.skip();
        stringReader.skipWhitespace();
        final Map<PosArgument, BlockPredicateArgument> map = new LinkedHashMap<>();
        while (true) {
          if (stringReader.canRead() && stringReader.peek() == '}') {
            stringReader.skip();
            break;
          }
          parser.suggestionProviders.clear();
          reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> parser.buildSuggestions(EntitySelectorReaderExtras.getOf(reader).context, suggestionsBuilder));
          final PosArgument posArgument = parser.parseAndSuggestArgument(EnhancedPosArgumentType.blockPos());

          stringReader.skipWhitespace();
          if (stringReader.canRead() && stringReader.peek() == '=') {
            stringReader.skip();
            stringReader.skipWhitespace();
          } else {
            reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest("=").buildFuture());
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(stringReader, "=");
          }

          parser.suggestionProviders.clear();
          reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> parser.buildSuggestions(EntitySelectorReaderExtras.getOf(reader).context, suggestionsBuilder));
          final BlockPredicateArgument blockPredicateArgument = BlockPredicateArgument.parse(MixinShared.getCommandRegistryAccess(), parser, false);

          map.put(posArgument, blockPredicateArgument);
          stringReader.skipWhitespace();

          reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest(",").suggest("}").buildFuture());
          if (stringReader.canRead() && stringReader.peek() == ',') {
            stringReader.skip();
            stringReader.skipWhitespace();
          } else if (stringReader.canRead() && stringReader.peek() == '}') {
            stringReader.skip();
            break;
          } else {
            throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(stringReader, ",", "}");
          }
        }

        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(source -> {
          final ImmutableMap.Builder<PosArgument, BlockPredicate> newMapBuilder = new ImmutableMap.Builder<>();
          for (final var entry : map.entrySet()) {
            final var posArgument = entry.getKey();
            final var blockPredicateArgument = entry.getValue();
            newMapBuilder.put(posArgument, blockPredicateArgument.apply(source));
          }
          return new BlockPredicatesEntityPredicateEntry(newMapBuilder.build());
        });
      } else {
        parser.suggestionProviders.clear();
        reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> parser.buildSuggestions(EntitySelectorReaderExtras.getOf(reader).context, suggestionsBuilder));
        final BlockPredicateArgument parse = BlockPredicateArgument.parse(MixinShared.getCommandRegistryAccess(), parser, false);
        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(source -> new BlockPredicateEntityPredicateEntry(parse.apply(source)));
      }
    }, Predicates.alwaysTrue(), Text.translatable("enhanced_commands.argument.entity_predicate.block"));

    // 检测实体所拥有的效果
    putOption("effect", reader -> {
      final StringReader stringReader = reader.getReader();
      stringReader.skipWhitespace();
      final RegistryWrapper<StatusEffect> wrapper = MixinShared.getCommandRegistryAccess().createWrapper(RegistryKeys.STATUS_EFFECT);
      final var type = new RegistryEntryArgumentType<>(MixinShared.getCommandRegistryAccess(), RegistryKeys.STATUS_EFFECT);
      if (stringReader.canRead() && stringReader.peek() == '{') {
        stringReader.skip();
        stringReader.skipWhitespace();
        final LinkedHashMap<StatusEffect, EntityEffectPredicate.EffectData> effects = new LinkedHashMap<>();
        final HashSet<StatusEffect> inverted = new HashSet<>();
        final EffectsEntityPredicateEntry entry = new EffectsEntityPredicateEntry(effects, inverted);
        while (true) {
          if (stringReader.canRead() && stringReader.peek() == '}') {
            stringReader.skip();
            break;
          }

          final int cursorBeforeEffectEntry = stringReader.getCursor();
          reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> CommandSource.suggestFromIdentifier(wrapper.filter(effect -> !effects.containsKey(effect)).streamEntries()::iterator, suggestionsBuilder, ref -> ref.registryKey().getValue(), ref1 -> ref1.value().getName()));
          final var effect = type.parse(stringReader).value();
          if (effects.containsKey(effect)) {
            final int cursorAfterEffectId = stringReader.getCursor();
            stringReader.setCursor(cursorBeforeEffectEntry);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(stringReader, effect.getName()), cursorAfterEffectId);
          }
          reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest("=").buildFuture());
          stringReader.skipWhitespace();
          stringReader.expect('=');
          final boolean isInverted = reader.readNegationCharacter();
          reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> CommandSource.suggestMatching(new String[]{"true", "false"}, suggestionsBuilder));

          // 这里暂时只允许读布尔值。
          final boolean expected = stringReader.readBoolean();
          effects.put(effect, new EntityEffectPredicate.EffectData());
          if (isInverted == expected) {
            inverted.add(effect);
          }

          stringReader.skipWhitespace();
          reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest(",").suggest("}").buildFuture());
          if (stringReader.canRead() && stringReader.peek() == ',') {
            stringReader.skip();
            stringReader.skipWhitespace();
          } else if (stringReader.canRead() && stringReader.peek() == '}') {
            stringReader.skip();
            break;
          } else {
            throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(stringReader, ",", "}");
          }
        }

        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(entry);
      } else {
        final boolean inverted = reader.readNegationCharacter();
        reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> CommandSource.suggestFromIdentifier(wrapper.streamEntries(), suggestionsBuilder, ref -> ref.registryKey().getValue(), ref -> ref.value().getName()));
        final StatusEffect value = type.parse(stringReader).value();

        EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new EffectEntityPredicateEntry(value, inverted));
      }
    }, reader -> isNeverPositivelyUsed(reader, "effect"), Text.translatable("enhanced_commands.argument.entity_predicate.effect"));
  }

  private static void putOption(String id, EntitySelectorOptions.SelectorHandler handler, Predicate<EntitySelectorReader> condition, Text description) {
    EntitySelectorOptionsAccessor.callPutOption(id, handler, condition, description);
  }

  private static void putSimpleBooleanOption(String id, Predicate<Entity> predicate) {
    putSimpleBooleanOption(id, predicate, "enhanced_commands.argument.entity_predicate." + id);
    markRequiringUnique(id);
  }

  private static void putSimpleBooleanOption(String id, Predicate<Entity> predicate, String baseTranslationKey) {
    putOption(id, reader -> {
      final boolean inverted = reader.readNegationCharacter();
      final boolean expected = inverted != reader.getReader().readBoolean();
      EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new SimpleBooleanEntityPredicateEntry(predicate, expected, baseTranslationKey + "." + true, baseTranslationKey + "." + false, id));
      // 对于布尔值，使用否定的直接替换其效果，仍视为未被取反的谓词
      markParamAsUsed(reader, id, false);
    }, reader -> isNeverPositivelyUsed(reader, id), Text.translatable(baseTranslationKey));
  }

  private static boolean markParamAsUsed(EntitySelectorReader reader, String option, boolean inverted) {
    return ((EntitySelectorReaderExtension) reader).ec$getExt().usedParams.put(option, inverted);
  }

  /**
   * 参数从未被以非反向的方式使用过。如果参数是以反向的方式使用的，则没有影响。
   */
  private static boolean isNeverPositivelyUsed(EntitySelectorReader reader, String option) {
    return ((EntitySelectorReaderExtension) reader).ec$getExt().usedParams.getOrDefault(option, true);
  }

  /**
   * 检查选择器中是否存在混合使用正向和反向的用法，例如，如果有 {@code key=!value1,key=value2}，那么解析到value2时就应该报错，因为这种情况下只能接受反向的用法。其他情况则不进行操作。
   *
   * @throws CommandSyntaxException 如果此前使用了反向的用法而当前不是反向的。
   */
  @Contract(pure = true)
  private static void checkNoInversionMix(EntitySelectorReader reader, String option, boolean inverted) throws CommandSyntaxException {
    final EntitySelectorReaderExtras extras = ((EntitySelectorReaderExtension) reader).ec$getExt();
    final Object2BooleanMap<String> usedParams = extras.usedParams;
    if (usedParams.getOrDefault(option, false)) {
      // 此前使用了反向的用法，则此时也必须要求使用反向的用法。
      if (!inverted) {
        final StringReader stringReader = reader.getReader();
        stringReader.setCursor(extras.cursorBeforeOptionName);
        throw CommandSyntaxExceptionExtension.withCursorEnd(MIXED_OPTION_INVERSION.createWithContext(stringReader, option), extras.cursorAfterOptionName);
      }
    }
  }

  public static void init() {
    registerInapplicableReasons();
    registerOptionAliases();
    registerModOptions();
    Validate.notEmpty(INAPPLICABLE_REASONS);
    Validate.notEmpty(OPTION_NAME_ALIASES);
  }

  /**
   * 此方法用于 mixin。此方法同时还会添加游戏模式谓词描述。
   */
  @ApiStatus.Internal
  public static boolean mixinReadMultipleTypes(EntitySelectorReader reader, boolean hasNegation, @NotNull Either<EntityType<?>, TagKey<EntityType<?>>> already) throws CommandSyntaxException {
    final StringReader stringReader = reader.getReader();
    final int cursorBeforeWhite = stringReader.getCursor();
    stringReader.skipWhitespace();

    if (stringReader.canRead() && stringReader.peek() == '|') {
      Set<EntityType<?>> parsedTypes = new HashSet<>();
      Set<TagKey<EntityType<?>>> parsedTypeKeys = new HashSet<>();
      already.ifLeft(parsedTypes::add);
      already.ifRight(parsedTypeKeys::add);
      List<Either<EntityType<?>, TagKey<EntityType<?>>>> values = Lists.newArrayList(already);
      while (stringReader.canRead() && stringReader.peek() == '|') {
        stringReader.skip();
        stringReader.skipWhitespace();
        final int cursorBeforeNext = stringReader.getCursor();

        // 提供游戏模式（包括本模组中提供的 a、1 等名称）的建议。
        reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> {
          CommandSource.suggestFromIdentifier(Registries.ENTITY_TYPE.streamEntries().filter(r -> !parsedTypes.contains(r.value())), suggestionsBuilder, r -> r.registryKey().getValue(), r -> r.value().getName());
          return CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.streamTags().filter(tagKey -> !parsedTypeKeys.contains(tagKey)).map(TagKey::id), suggestionsBuilder, "#");
        });

        if (reader.readTagCharacter()) {
          TagKey<EntityType<?>> tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.fromCommandInput(reader.getReader()));
          final var suggestionProvider = ((EntitySelectorReaderAccessor) reader).getSuggestionProvider();
          reader.setSuggestionProvider((builder, suggestionsBuilderConsumer) -> suggestionProvider.apply(builder.createOffset(cursorBeforeNext), suggestionsBuilderConsumer).thenCombine(builder.suggest(",").suggest("]").buildFuture(), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions));
          if (!stringReader.canRead()) {
            throw EntitySelectorReader.UNTERMINATED_EXCEPTION.create();
          }
          if (parsedTypeKeys.contains(tagKey)) {
            final int cursorAfterNext = stringReader.getCursor();
            stringReader.setCursor(cursorBeforeNext);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(stringReader, "#" + tagKey.id()), cursorAfterNext);
          }
          parsedTypeKeys.add(tagKey);
          values.add(Either.right(tagKey));
        } else {
          Identifier identifier = Identifier.fromCommandInput(reader.getReader());
          final int cursorAfterNext = stringReader.getCursor();
          EntityType<?> entityType = Registries.ENTITY_TYPE.getOrEmpty(identifier).orElseThrow(() -> {
            stringReader.setCursor(cursorBeforeNext);
            return CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptions.INVALID_TYPE_EXCEPTION.createWithContext(stringReader, identifier.toString()), cursorAfterNext);
          });
          if (parsedTypes.contains(entityType)) {
            stringReader.setCursor(cursorBeforeNext);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(stringReader, entityType.getName()), cursorAfterNext);
          }
          parsedTypes.add(entityType);
          values.add(Either.left(entityType));
        }

        stringReader.skipWhitespace();
        // 由于有明确的定界符，因此此处的 skipWhitespace 是安全的。
      }

      EntitySelectorReaderExtras.getOf(reader).addPredicateAndDescription(new TypesEntityPredicateEntry(values, hasNegation));
      return true;
    } else {
      stringReader.setCursor(cursorBeforeWhite);
      return false;
    }
  }

  /**
   * 此方法用于 mixin。此方法同时还会添加游戏模式谓词描述。
   *
   * @return 如果为 {@code true}，则使用和原版一致的方法，否则需要在此方法中进行相应操作并抑制原版方法中的操作。
   * @see EntitySelectorOptionsMixin#readMultipleGameModes(EntitySelectorReader, Predicate, boolean, GameMode)
   */
  @ApiStatus.Internal
  public static boolean mixinReadMultipleGameModes(EntitySelectorReader reader, boolean hasNegation, @NotNull GameMode gameMode) throws CommandSyntaxException {
    final StringReader stringReader = reader.getReader();
    final int cursorBeforeWhite = stringReader.getCursor();
    stringReader.skipWhitespace();

    if (stringReader.canRead() && stringReader.peek() == '|') {
      // 解析更多的游戏模式，例如：
      // m = c <当前cursor> | a | sp
      EnumSet<GameMode> parsedGameModes = EnumSet.of(gameMode);
      while (stringReader.canRead() && stringReader.peek() == '|') {
        stringReader.skip();
        stringReader.skipWhitespace();
        final int cursorBeforeNext = stringReader.getCursor();

        // 提供游戏模式（包括本模组中提供的 a、1 等名称）的建议。
        reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> CommandSource.suggestMatching(Stream.concat(Arrays.stream(GameMode.values()).filter(m -> !parsedGameModes.contains(m)).map(GameMode::getName), MixinShared.EXTENDED_GAME_MODE_NAMES.entrySet().stream().filter(entry -> !parsedGameModes.contains(entry.getValue())).map(Map.Entry::getKey)), suggestionsBuilder));

        final String nextString = stringReader.readUnquotedString();
        final GameMode next = GameMode.byName(nextString, null);
        final int cursorAfterNext = stringReader.getCursor();
        if (next == null) {
          stringReader.setCursor(cursorBeforeNext);
          throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptions.INVALID_MODE_EXCEPTION.createWithContext(stringReader, nextString), cursorAfterNext);
        } else if (parsedGameModes.contains(next)) {
          stringReader.setCursor(cursorBeforeNext);
          throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(stringReader, nextString), cursorAfterNext);
        } else {
          parsedGameModes.add(next);
        }

        stringReader.skipWhitespace();
        // 由于有明确的定界符，因此此处的 skipWhitespace 是安全的。
      }

      reader.setPredicate(entity -> {
        if (entity instanceof final ServerPlayerEntity serverPlayerEntity) {
          GameMode actualGameMode = serverPlayerEntity.interactionManager.getGameMode();
          return hasNegation != parsedGameModes.contains(actualGameMode);
        } else {
          return false;
        }
      });
      EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new GameModeEntityPredicateEntry.Multiple(parsedGameModes, hasNegation));
      return false;
    } else {
      stringReader.setCursor(cursorBeforeWhite);
      EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new GameModeEntityPredicateEntry.Single(gameMode, hasNegation));
      return true;
    }
  }

  /**
   * 此方法用于辅助 {@link EntitySelectorOptionsMixin} 中的 mixin。
   */
  @ApiStatus.Internal
  public static void mixinGetScoreSuggestions(EntitySelectorReader entitySelectorReader, StringReader stringReader, @NotNull CommandContext<?> context) {
    if (context.getSource() instanceof final ServerCommandSource serverCommandSource) {
      final int cursor = stringReader.getCursor();
      final ServerScoreboard scoreboard = serverCommandSource.getServer().getScoreboard();
      final Collection<String> objectiveNames = scoreboard.getObjectiveNames();
      entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> ParsingUtil.suggestMatchingStringWithTooltip(objectiveNames, s -> scoreboard.getObjective(s).getDisplayName(), suggestionsBuilder.createOffset(cursor)));
    } else if (context.getSource() instanceof final CommandSource commandSource) {
      entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.getCompletions(context));
    }
  }

  /**
   * 此方法用于辅助 {@link EntitySelectorOptionsMixin} 中的 mixin。
   */
  @ApiStatus.Internal
  public static void mixinGetAdvancementIdSuggestions(EntitySelectorReader entitySelectorReader, StringReader stringReader, @NotNull CommandContext<?> context) {
    if (context.getSource() instanceof final ServerCommandSource serverCommandSource) {
      final int cursor = stringReader.getCursor();
      final var advancementLoader = serverCommandSource.getServer().getAdvancementLoader();
      entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> {
        final SuggestionsBuilder offset = suggestionsBuilder.createOffset(cursor);
        CommandSource.suggestFromIdentifier(advancementLoader.getAdvancements(), offset, Advancement::getId, Advancement::toHoverableText);
        return offset.buildFuture();
      });
    } else if (context.getSource() instanceof final CommandSource commandSource) {
      entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.getCompletions(context));
    }
  }

  /**
   * 此方法用于辅助 {@link EntitySelectorOptionsMixin} 中的 mixin。
   */
  @ApiStatus.Internal
  public static void mixinGetLootConditionIdSuggestions(EntitySelectorReader entitySelectorReader, StringReader stringReader, @NotNull CommandContext<?> context) {
    if (context.getSource() instanceof final ServerCommandSource serverCommandSource) {
      final int cursor = stringReader.getCursor();
      final var predicateManager = serverCommandSource.getServer().getPredicateManager();
      entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> CommandSource.suggestIdentifiers(predicateManager.getIds(), suggestionsBuilder.createOffset(cursor)));
    } else if (context.getSource() instanceof final CommandSource commandSource) {
      entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.getCompletions(context));
    }
  }

  /**
   * 此方法用于辅助 {@link EntitySelectorOptionsMixin} 中的 mixin，返回的谓词仅测试被反向的分数条件。
   */
  @NotNull
  public static Predicate<Entity> mixinInvertedScoredPredicate(List<Pair<String, NumberRange.IntRange>> invertedScores) {
    return entity -> {
      final Scoreboard scoreboard = entity.getServer().getScoreboard();
      final String entityName = entity.getEntityName();
      for (Pair<String, NumberRange.IntRange> pair : invertedScores) {
        ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(pair.left());
        if (scoreboardObjective == null) {
          return false;
        }
        if (!scoreboard.playerHasObjective(entityName, scoreboardObjective)) {
          return false;
        }
        ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(entityName, scoreboardObjective);
        int i = scoreboardPlayerScore.getScore();
        if (pair.right().test(i)) {
          return false;
        }
      }
      return true;
    };
  }

  protected static final Gson LOOT_CONDITION_GSON = LootGsons.getConditionGsonBuilder().setLenient().create();

  public static boolean mixinReadLiteralPredicate(EntitySelectorReader reader, boolean hasNegation, StringReader stringReader) throws CommandSyntaxException {
    reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> {
      final CommandContext<?> context = EntitySelectorReaderExtras.getOf(reader).context;
      if (context != null && context.getSource() instanceof ServerCommandSource source) {
        LootConditionManager lootConditionManager = source.getServer().getPredicateManager();
        return CommandSource.suggestIdentifiers(lootConditionManager.getIds(), suggestionsBuilder);
      } else if (context != null && context.getSource() instanceof CommandSource commandSource) {
        return commandSource.getCompletions(context);
      } else {
        return Suggestions.empty();
      }
    });
    if (stringReader.canRead() && stringReader.peek() == '{') {
      reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
      final LootCondition lootCondition = ParsingUtil.parseJson(stringReader, input -> LOOT_CONDITION_GSON.fromJson(input, LootCondition.class), ModCommandExceptionTypes.INVALID_LOOT_TABLE_JSON);
      reader.setPredicate(entity -> {
        if (!(entity.world instanceof ServerWorld serverWorld)) {
          return false;
        } else {
          if (lootCondition == null) {
            return false;
          } else {
            LootContext lootContext = new LootContext.Builder(serverWorld)
                .parameter(LootContextParameters.THIS_ENTITY, entity)
                .parameter(LootContextParameters.ORIGIN, entity.getPos())
                .build(LootContextTypes.SELECTOR);
            return hasNegation ^ lootCondition.test(lootContext);
          }
        }
      });
      EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new LootTablePredicateAnonymousEntityPredicateEntry(lootCondition, hasNegation));
      return true;
    }
    return false;
  }

  @FunctionalInterface
  public interface InapplicableReasonProvider {
    @Nullable CommandSyntaxException getReason(EntitySelectorReader reader, String option, int restoreCursor);
  }
}
