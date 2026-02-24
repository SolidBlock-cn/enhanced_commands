package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.fabricmc.fabric.mixin.command.EntitySelectorOptionsAccessor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.MobEffectsPredicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.mixins.accessor.EntitySelectorParserAccessor;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.mixins.mixin.EntitySelectorOptionsMixin;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.RegionProvider;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;
import pers.solid.ecmd.util.bridge.BridgeIntRange;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EntitySelectorOptionsExtension {
  /**
   * 此映射用于对 {@link net.minecraft.commands.arguments.selector.options.EntitySelectorOptions#get(EntitySelectorParser, String, int)} 中遇到不要应用的选项时进行扩展，如果遇到了不可应用的选项，会尝试从此映射中查找不可应用的原因，而非简单的某选项不适用的消息。如果为 {@code null}，则不影响原版的行为。
   */
  public static final Map<String, InapplicableReasonProvider> INAPPLICABLE_REASONS = new HashMap<>();
  /**
   * 此映射用于选项名称的别称，当解析到不存在的选项名称时，会尝试解析到别称。
   */
  public static final Map<String, String> OPTION_NAME_ALIASES = new HashMap<>();
  /**
   * 此集包括需要在解析完成后推迟进入到下一步建议的选项名称的集合。例如，如果没有这个集合，在输入 {@code @a[not=Pla]} 之后，玩家名称“Pla”被视为解析完成，不再提供 {@code PlayerName} 的建议而直接进入到提示输入逗号或方括号。为了避免这一部分，此集合收集需要特定处理的选项名称。
   */
  public static final Set<String> INCOMPLETE_SUGGESTIONS = new HashSet<>();


  public static final DynamicCommandExceptionType DUPLICATE_OPTION = new DynamicCommandExceptionType(optionName -> Component.translatable("enhanced_commands.argument.entity.options.duplicate_option", optionName));
  public static final DynamicCommandExceptionType DUPLICATE_OPTION_WITHOUT_INVERSION = new DynamicCommandExceptionType(optionName -> Component.translatable("enhanced_commands.argument.entity.options.duplicate_option_without_inversion", optionName));
  public static final DynamicCommandExceptionType MIXED_OPTION_INVERSION = new DynamicCommandExceptionType(optionName -> Component.translatable("enhanced_commands.argument.entity.options.mixed_option_inversion", optionName));
  public static final SimpleCommandExceptionType INVALID_LIMIT_FOR_AT_S = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.entity.options.invalid_limit_for_@s"));
  public static final SimpleCommandExceptionType INVALID_SORT_FOR_AT_S = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.entity.options.invalid_sort_for_@s"));
  public static final DynamicCommandExceptionType INVALID_TYPE_FOR_SELECTOR = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.argument.entity.options.invalid_type_for_selector", o));
  public static final DynamicCommandExceptionType DISTANCE_ALREADY_EXPLICIT = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.argument.entity.options.distance_already_explicit", o));
  public static final SimpleCommandExceptionType DISTANCE_ALREADY_IMPLICIT = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.entity.options.distance_already_implicit"));
  public static final SimpleCommandExceptionType INVALID_NEGATIVE_LIMIT_WITH_SORTER = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.entity.options.invalid_negative_limit_with_sorter"));
  public static final SimpleCommandExceptionType INVALID_SORTER_WITH_NEGATIVE_LIMIT = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.entity.options.invalid_sorter_with_negative_limit"));
  public static final SimpleCommandExceptionType OWNER_OPTION_IN_PET_VARIABLE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.entity.options.owner_option_in_pets_variable"));
  /**
   * 在解析内容时提供布尔值的建议。
   */
  public static final BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> BOOLEAN_SUGGEST = (builder, suggestionsBuilderConsumer) -> ParsingUtil.suggestBoolean(builder);

  private static void registerInapplicableReasons() {
    final var map = INAPPLICABLE_REASONS;
    markRequiringUniqueNoMixture("propertyName");
    map.put("distance", (reader, option, restoreCursor) -> {
      reader.getReader().setCursor(restoreCursor);
      if (reader.extension$ec().implicitDistance) {
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
      return reader.isCurrentEntity() ? INVALID_LIMIT_FOR_AT_S.createWithContext(reader.getReader()) : DUPLICATE_OPTION.createWithContext(reader.getReader(), "limit");
    });
    map.put("sort", (reader, option, restoreCursor) -> {
      final StringReader stringReader = reader.getReader();
      stringReader.setCursor(restoreCursor);
      if (reader.extension$ec().implicitNegativeLimit) {
        return INVALID_SORTER_WITH_NEGATIVE_LIMIT.createWithContext(stringReader);
      }
      return reader.isCurrentEntity() ? INVALID_SORT_FOR_AT_S.createWithContext(stringReader) : DUPLICATE_OPTION.createWithContext(stringReader, "sort");
    });
    markRequiringUniqueNoMixture("gamemode");
    markRequiringUnique("team");
    map.put("type", (reader, option, restoreCursor) -> {
      if (reader.isTypeLimited()) {
        final EntitySelectorReaderExtras entitySelectorReaderExtras = reader.extension$ec();
        final String atVariable = entitySelectorReaderExtras.atVariable;
        final StringReader stringReader = reader.getReader();
        if (atVariable != null) {
          switch (atVariable) {
            case "a", "r", "p" -> {
              stringReader.setCursor(restoreCursor);
              return INVALID_TYPE_FOR_SELECTOR.createWithContext(stringReader, EntitySelectorParser.SYNTAX_SELECTOR_START + atVariable);
            }
          }
        }
        stringReader.skipWhitespace();
        if (stringReader.canRead() && stringReader.read() == EntitySelectorParser.SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR && reader.shouldInvertValue()) {
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
      if (reader.extension$ec().implicitDistance) {
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
      if (stringReader.canRead() && stringReader.read() == EntitySelectorParser.SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR) {
        stringReader.skipWhitespace();
        if (reader.shouldInvertValue()) {
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
      final MinMaxBounds.Doubles original = reader.getDistance();
      final StringReader stringReader = reader.getReader();
      final int cursorBeforeValue = stringReader.getCursor();
      final float value = stringReader.readFloat();
      reader.extension$ec().implicitDistance = true;
      if (original.min().isEmpty()) {
        reader.setDistance(MinMaxBounds.Doubles.atMost(value));
      } else {
        if (value < original.min().get()) {
          final int cursorAfterValue = stringReader.getCursor();
          stringReader.setCursor(cursorBeforeValue);
          throw CommandSyntaxExceptionExtension.withCursorEnd(MinMaxBounds.ERROR_SWAPPED.createWithContext(stringReader), cursorAfterValue);
        } else if (value < 0) {
          final int cursorAfterValue = stringReader.getCursor();
          stringReader.setCursor(cursorBeforeValue);
          throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptions.ERROR_RANGE_NEGATIVE.createWithContext(stringReader), cursorAfterValue);
        }
        reader.setDistance(MinMaxBounds.Doubles.between(original.min().get(), value));
      }
    }, reader -> reader.getDistance().isAny() || reader.extension$ec().implicitDistance && reader.getDistance().max().isPresent(), Component.translatable("enhanced_commands.argument.entity.options.r"));
    putOption("rm", reader -> {
      final MinMaxBounds.Doubles original = reader.getDistance();
      final StringReader stringReader = reader.getReader();
      final int cursorBeforeValue = stringReader.getCursor();
      final float value = stringReader.readFloat();
      reader.extension$ec().implicitDistance = true;
      if (original.max().isEmpty()) {
        reader.setDistance(MinMaxBounds.Doubles.atLeast(value));
      } else {
        if (value > original.max().get()) {
          final int cursorAfterValue = stringReader.getCursor();
          stringReader.setCursor(cursorBeforeValue);
          throw CommandSyntaxExceptionExtension.withCursorEnd(MinMaxBounds.ERROR_SWAPPED.createWithContext(stringReader), cursorAfterValue);
        } else if (value < 0) {
          final int cursorAfterValue = stringReader.getCursor();
          stringReader.setCursor(cursorBeforeValue);
          throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptions.ERROR_RANGE_NEGATIVE.createWithContext(stringReader), cursorAfterValue);
        }
        reader.setDistance(MinMaxBounds.Doubles.between(value, original.max().get()));
      }
    }, reader -> reader.getDistance().isAny() || reader.extension$ec().implicitDistance && reader.getDistance().max().isPresent(), Component.translatable("enhanced_commands.argument.entity.options.rm"));

    putOption("region", reader -> {
      final CommandBuildContext commandBuildContext = MixinShared.getCommandRegistryAccess();
      final ParseContext<Object> parseContext = new ParseContext<>(commandBuildContext, reader.getReader(), false, true);
      //noinspection unchecked
      reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> parseContext.buildSuggestions((CommandContext<Object>) reader.extension$ec().context, suggestionsBuilder));
      final RegionProvider<?> regionProvider = RegionProvider.parse(parseContext);

      reader.addPredicate(new RegionEntityPredicateEntry(regionProvider));
    }, Predicates.alwaysTrue(), Component.translatable("enhanced_commands.entity_predicate.region.option_name"));

    putOption("alternatives", reader -> {
      final boolean inverted = reader.shouldInvertValue();
      final StringReader stringReader = reader.getReader();
      reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest("[").buildFuture());
      if (stringReader.canRead() && stringReader.peek() == '[') {
        stringReader.skip();
        stringReader.skipWhitespace();
      } else {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(stringReader, "[");
      }

      // 在左方括号的后面，开始解析实体谓词。
      ImmutableList.Builder<EntityPredicate> entityPredicates = new ImmutableList.Builder<>();
      while (true) {
        stringReader.skipWhitespace();
        if (stringReader.canRead() && stringReader.peek() == ']') {
          stringReader.skip();
          break;
        }

        final EntitySelectorParser newReader = new EntitySelectorParser(stringReader, true);
        final int cursorBeforeRead = stringReader.getCursor();
        try {
          entityPredicates.add(EntityPredicate.parse(newReader));
        } catch (CommandSyntaxException e) {
          reader.setSuggestions((builder, consumer) -> newReader.fillSuggestions(builder, suggestionsBuilder -> {
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

        reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> {
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

      final ImmutableList<EntityPredicate> build = entityPredicates.build();
      reader.addPredicate(new AlternativesEntityPredicateEntry(build, inverted));
    }, Predicates.alwaysTrue(), Component.translatable("enhanced_commands.argument.entity.options.alternatives"));

    putOption("health", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.shouldInvertValue();
      checkNoInversionMix(reader, "health", inverted);
      final int cursorBefore = stringReader.getCursor();
      reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> ParsingUtil.suggestString("max", suggestionsBuilder).buildFuture());
      final String unquotedString = stringReader.readUnquotedString();
      if ("max".equals(unquotedString)) {
        reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
        reader.addPredicate(new HealthMaxEntityPredicateEntry(inverted));
      } else {
        stringReader.setCursor(cursorBefore);
        final BridgeFloatRange floatRange = BridgeFloatRange.parse(stringReader);
        reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
        reader.addPredicate(new HealthEntityPredicateEntry(floatRange, inverted));
      }
      markParamAsUsed(reader, "health", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "health"), Component.translatable("enhanced_commands.entity_predicate.health.option_name"));
    markRequiringUniqueNoMixture("health");
    putOption("air", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.shouldInvertValue();
      checkNoInversionMix(reader, "air", inverted);
      final int cursorBefore = stringReader.getCursor();
      reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> ParsingUtil.suggestString("max", suggestionsBuilder).buildFuture());
      final String unquotedString = stringReader.readUnquotedString();
      if ("max".equals(unquotedString)) {
        reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
        reader.addPredicate(new AirMaxEntityPredicateEntry(inverted));
      } else {
        stringReader.setCursor(cursorBefore);
        final BridgeIntRange intRange = BridgeIntRange.parse(stringReader);
        reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
        reader.addPredicate(new AirEntityPredicateEntry(intRange, inverted));
      }
      markParamAsUsed(reader, "air", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "air"), Component.translatable("enhanced_commands.entity_predicate.air.option_name"));
    markRequiringUniqueNoMixture("air");
    putOption("food", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.shouldInvertValue();
      checkNoInversionMix(reader, "food", inverted);
      final BridgeIntRange intRange = BridgeIntRange.parse(stringReader);
      reader.setIncludesEntities(false);
      reader.addPredicate(new FoodEntityPredicateEntry(intRange, inverted));
      markParamAsUsed(reader, "food", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "food"), Component.translatable("enhanced_commands.entity_predicate.food.option_name"));
    markRequiringUniqueNoMixture("food");
    putOption("saturation", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.shouldInvertValue();
      checkNoInversionMix(reader, "saturation", inverted);
      final BridgeFloatRange floatRange = BridgeFloatRange.parse(stringReader);
      reader.setIncludesEntities(false);
      reader.addPredicate(new SaturationEntityPredicateEntry(floatRange, inverted));
      markParamAsUsed(reader, "saturation", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "saturation"), Component.translatable("enhanced_commands.entity_predicate.saturation.option_name"));
    markRequiringUniqueNoMixture("saturation");
    putOption("exhaustion", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.shouldInvertValue();
      checkNoInversionMix(reader, "exhaustion", inverted);
      final BridgeFloatRange floatRange = BridgeFloatRange.parse(stringReader);
      reader.setIncludesEntities(false);
      reader.addPredicate(new ExhaustionEntityPredicateEntry(floatRange, inverted));
      markParamAsUsed(reader, "exhaustion", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "exhaustion"), Component.translatable("enhanced_commands.entity_predicate.exhaustion.option_name"));
    markRequiringUniqueNoMixture("exhaustion");
    putOption("fire", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.shouldInvertValue();
      checkNoInversionMix(reader, "fire", inverted);
      final BridgeIntRange intRange = BridgeIntRange.parse(stringReader);
      reader.addPredicate(new FireEntityPredicateEntry(intRange, inverted));
      markParamAsUsed(reader, "fire", inverted);
    }, reader -> isNeverPositivelyUsed(reader, "fire"), Component.translatable("enhanced_commands.entity_predicate.fire.option_name"));
    markRequiringUniqueNoMixture("fire");

    putOption("pose", reader -> {
      final boolean inverted = reader.shouldInvertValue();
      checkNoInversionMix(reader, "pose", inverted);
      final int cursorBefore = reader.getReader().getCursor();
      reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> SharedSuggestionProvider.suggest(PoseEntityPredicateEntry.ENTITY_POSE_NAMES.values(), suggestionsBuilder));
      final String s = reader.getReader().readUnquotedString();
      final Pose entityPose = PoseEntityPredicateEntry.ENTITY_POSE_NAMES.inverse().get(s);
      if (entityPose != null) {
        reader.addPredicate(new PoseEntityPredicateEntry(entityPose, inverted));
        markParamAsUsed(reader, "pose", inverted);
      } else {
        final int cursorAfter = reader.getReader().getCursor();
        reader.getReader().setCursor(cursorBefore);
        throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader.getReader()), cursorAfter);
      }
    }, reader -> isNeverPositivelyUsed(reader, "pose"), Component.translatable("enhanced_commands.entity_predicate.pose"));
    markRequiringUniqueNoMixture("pose");

    // 以下对应 EntityFlags 部分

    putSimpleBooleanOption("on_fire", EntityPredicateTypes.ON_FIRE);
    // 注意：这里的 isSneaking 于 EntityFlagsPredicate 所使用的 isInSneakingPose 不同
    putSimpleBooleanOption("sneaking", EntityPredicateTypes.SNEAKING);
    putSimpleBooleanOption("sprinting", EntityPredicateTypes.SPRINTING);
    putSimpleBooleanOption("swimming", EntityPredicateTypes.SWIMMING);
    putSimpleBooleanOption("baby", EntityPredicateTypes.BABY);

    // 检测实体所在的方块
    putOption("block", reader -> {
      final StringReader stringReader = reader.getReader();
      stringReader.skipWhitespace();
      final ParseContext<Object> parseContext = new ParseContext<>(stringReader);
      if (stringReader.canRead() && stringReader.peek() == '{') {
        stringReader.skip();
        stringReader.skipWhitespace();
        final List<com.mojang.datafixers.util.Pair<EnhancedCoordinates, BlockPredicate>> list = new ArrayList<>();
        while (true) {
          if (stringReader.canRead() && stringReader.peek() == '}') {
            stringReader.skip();
            break;
          }
          parseContext.clearSuggestion();
          //noinspection unchecked
          reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> parseContext.buildSuggestions((CommandContext<Object>) reader.extension$ec().context, suggestionsBuilder));
          final EnhancedCoordinates posArgument = parseContext.parseAndSuggestArgument(EnhancedPosArgument.blockPos());

          stringReader.skipWhitespace();
          if (stringReader.canRead() && stringReader.peek() == '=') {
            stringReader.skip();
            stringReader.skipWhitespace();
          } else {
            reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest("=").buildFuture());
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(stringReader, "=");
          }

          parseContext.clearSuggestion();
          //noinspection unchecked
          reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> parseContext.buildSuggestions((CommandContext<Object>) reader.extension$ec().context, suggestionsBuilder));
          final BlockPredicate blockPredicate = BlockPredicate.parse(new ParseContext<>(MixinShared.getCommandRegistryAccess(), parseContext.reader(), parseContext.suggestions(), false, true));

          list.add(new com.mojang.datafixers.util.Pair<>(posArgument, blockPredicate));
          stringReader.skipWhitespace();

          reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest(",").suggest("}").buildFuture());
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

        reader.addPredicate(new BlockPredicatesEntityPredicateEntry(list));
      } else {
        parseContext.clearSuggestion();
        //noinspection unchecked
        reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> parseContext.buildSuggestions((CommandContext<Object>) reader.extension$ec().context, suggestionsBuilder));
        final BlockPredicate parse = BlockPredicate.parse(new ParseContext<>(MixinShared.getCommandRegistryAccess(), parseContext.reader(), parseContext.suggestions(), false, true));
        reader.addPredicate(new BlockPredicateEntityPredicateEntry(parse));
      }
    }, Predicates.alwaysTrue(), Component.translatable("enhanced_commands.entity_predicate.block"));

    // 检测实体所拥有的效果
    putOption("effect", reader -> {
      final StringReader stringReader = reader.getReader();
      stringReader.skipWhitespace();
      final HolderLookup<MobEffect> wrapper = MixinShared.getCommandRegistryAccess().lookupOrThrow(Registries.MOB_EFFECT);
      final var type = ResourceArgument.resource(MixinShared.getCommandRegistryAccess(), Registries.MOB_EFFECT);
      if (stringReader.canRead() && stringReader.peek() == '{') {
        stringReader.skip();
        stringReader.skipWhitespace();
        final ImmutableList.Builder<EffectsEntityPredicateEntry.Entry> effects = new ImmutableList.Builder<>();
        final Set<Holder<MobEffect>> usedEffects = new HashSet<>();
        while (true) {
          if (stringReader.canRead() && stringReader.peek() == '}') {
            stringReader.skip();
            break;
          }

          final int cursorBeforeEffectEntry = stringReader.getCursor();
          reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> SharedSuggestionProvider.suggestResource(wrapper.listElements().filter(effectEntry -> !usedEffects.contains(effectEntry)), suggestionsBuilder, ref -> ref.key().location(), ref1 -> ref1.value().getDisplayName()));
          final var effectEntry = type.parse(stringReader);
          if (usedEffects.contains(effectEntry)) {
            final int cursorAfterEffectId = stringReader.getCursor();
            stringReader.setCursor(cursorBeforeEffectEntry);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(stringReader, effectEntry.value().getDisplayName()), cursorAfterEffectId);
          }
          reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest("=").buildFuture());
          stringReader.skipWhitespace();
          stringReader.expect('=');
          final boolean isInverted = reader.shouldInvertValue();
          reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> SharedSuggestionProvider.suggest(new String[]{"true", "false"}, suggestionsBuilder));

          // 这里暂时只允许读布尔值。
          final boolean expected = stringReader.readBoolean();
          usedEffects.add(effectEntry);
          effects.add(new EffectsEntityPredicateEntry.Entry(effectEntry, new MobEffectsPredicate.MobEffectInstancePredicate(), isInverted != expected));

          stringReader.skipWhitespace();
          reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> suggestionsBuilder.suggest(",").suggest("}").buildFuture());
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
        final EffectsEntityPredicateEntry entry = new EffectsEntityPredicateEntry(effects.build());

        reader.addPredicate(entry);
      } else {
        final boolean inverted = reader.shouldInvertValue();
        reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> SharedSuggestionProvider.suggestResource(wrapper.listElements(), suggestionsBuilder, ref -> ref.key().location(), ref -> ref.value().getDisplayName()));
        final Holder.Reference<MobEffect> value = type.parse(stringReader);

        reader.addPredicate(new EffectEntityPredicateEntry(value, inverted));
      }
    }, reader -> isNeverPositivelyUsed(reader, "effect"), Component.translatable("enhanced_commands.entity_predicate.effect"));

    // 检测实体的主人（驯养者）
    putOption("owner", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.shouldInvertValue();
      stringReader.skipWhitespace();
      if (stringReader.canRead()) {
        final char peek = stringReader.peek();
        if (peek == ']' || peek == ',' || peek == ';') {
          // 在 'owner=' 或 'owner=!' 后没有接任何值时，使用 null 值
          reader.addPredicate(new OwnerEntityPredicateEntry(null, inverted));
        } else {
          final EntitySelectorParser newReader = new EntitySelectorParser(reader.getReader(), true);
          reader.setSuggestions(newReader::fillSuggestions);
          reader.addPredicate(new OwnerEntityPredicateEntry(EntityPredicate.parse(newReader), inverted));
        }
      }
    }, reader -> !EntitySelectorTypeExtras.PETS.equals(reader.extension$ec().atVariable), Component.translatable("enhanced_commands.entity_predicate.owner"));
    INCOMPLETE_SUGGESTIONS.add("owner");
    INAPPLICABLE_REASONS.put("owner", (reader, option, restoreCursor) -> {
      reader.getReader().setCursor(restoreCursor);
      return OWNER_OPTION_IN_PET_VARIABLE.createWithContext(reader.getReader());
    });

    // 用于在带有特定的收集器的情况下，指定收集基于哪个实体的实体
    putOption("of", reader -> {
      final EntitySelectorParser subReader = new EntitySelectorParser(reader.getReader(), true);
      reader.setSuggestions(subReader::fillSuggestions);
      reader.extension$ec().collectorOf = subReader.parse();
    }, reader -> isNeverPositivelyUsed(reader, "of"), Component.translatable("enhanced_commands.entity_predicate.of"));
    markRequiringUnique("of");
    INCOMPLETE_SUGGESTIONS.add("of");

    // 用于 is 和 not
    putOption("is", reader -> {
      final boolean inverted = reader.shouldInvertValue();

      final EntitySelectorParser newReader = new EntitySelectorParser(reader.getReader(), true);
      reader.setSuggestions(newReader::fillSuggestions);
      reader.addPredicate(new SubPredicateEntityPredicateEntry(EntityPredicate.parse(newReader), inverted));
    }, Predicates.alwaysTrue(), Component.translatable("enhanced_commands.entity_predicate.sub_predicate"));
    INCOMPLETE_SUGGESTIONS.add("is");
    putOption("not", reader -> {
      final boolean inverted = reader.shouldInvertValue();

      final EntitySelectorParser newReader = new EntitySelectorParser(reader.getReader(), true);
      reader.setSuggestions(newReader::fillSuggestions);
      reader.addPredicate(new SubPredicateEntityPredicateEntry(EntityPredicate.parse(newReader), !inverted));
    }, Predicates.alwaysTrue(), Component.translatable("enhanced_commands.entity_predicate.sub_predicate.inverted"));
    INCOMPLETE_SUGGESTIONS.add("not");
  }

  @ApiStatus.Internal
  public static BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> getPutOffSuggestions(BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> original, BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> next) {
    return (suggestionsBuilder, suggestionsBuilderConsumer) -> original.apply(suggestionsBuilder, suggestionsBuilderConsumer).thenCombine(next.apply(suggestionsBuilder, suggestionsBuilderConsumer), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions);
  }

  private static void putOption(String id, EntitySelectorOptions.Modifier handler, Predicate<EntitySelectorParser> condition, Component description) {
    EntitySelectorOptionsAccessor.callPutOption(id, handler, condition, description);
  }


  private static void putSimpleBooleanOption(String id, SimpleBooleanEntityPredicateType type) {
    putOption(id, reader -> {
      final boolean inverted = reader.shouldInvertValue();
      reader.setSuggestions(BOOLEAN_SUGGEST);
      final boolean expected = inverted != reader.getReader().readBoolean();
      reader.addPredicate(new SimpleBooleanEntityPredicateEntry(type, expected));
      // 对于布尔值，使用否定的直接替换其效果，仍视为未被取反的谓词
      markParamAsUsed(reader, id, false);
    }, reader -> isNeverPositivelyUsed(reader, id), Component.translatable(type.baseTranslationKey));
    markRequiringUnique(id);
  }

  private static void markParamAsUsed(EntitySelectorParser reader, String option, boolean inverted) {
    reader.extension$ec().usedParams.put(option, inverted);
  }

  /**
   * 参数从未被以非反向的方式使用过。如果参数是以反向的方式使用的，则没有影响。
   */
  private static boolean isNeverPositivelyUsed(EntitySelectorParser reader, String option) {
    return reader.extension$ec().usedParams.getOrDefault(option, true);
  }

  /**
   * 检查选择器中是否存在混合使用正向和反向的用法，例如，如果有 {@code key=!value1,key=value2}，那么解析到value2时就应该报错，因为这种情况下只能接受反向的用法。其他情况则不进行操作。
   *
   * @throws CommandSyntaxException 如果此前使用了反向的用法而当前不是反向的。
   */
  @Contract(pure = true)
  private static void checkNoInversionMix(EntitySelectorParser reader, String option, boolean inverted) throws CommandSyntaxException {
    final EntitySelectorReaderExtras extras = reader.extension$ec();
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
  public static boolean mixinReadMultipleTypes(EntitySelectorParser reader, boolean inverted, @NotNull Either<EntityType<?>, TagKey<EntityType<?>>> already) throws CommandSyntaxException {
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
        reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> {
          SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.holders().filter(r -> !parsedTypes.contains(r.value())), suggestionsBuilder, r -> r.key().location(), r -> r.value().getDescription());
          return SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.getTagNames().filter(tagKey -> !parsedTypeKeys.contains(tagKey)).map(TagKey::location), suggestionsBuilder, "#");
        });

        if (reader.isTag()) {
          TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.read(reader.getReader()));
          final var suggestionProvider = ((EntitySelectorParserAccessor) reader).getSuggestions();
          reader.setSuggestions((builder, suggestionsBuilderConsumer) -> suggestionProvider.apply(builder.createOffset(cursorBeforeNext), suggestionsBuilderConsumer).thenCombine(builder.suggest(",").suggest("]").buildFuture(), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions));
          if (!stringReader.canRead()) {
            throw EntitySelectorParser.ERROR_EXPECTED_END_OF_OPTIONS.create();
          }
          if (parsedTypeKeys.contains(tagKey)) {
            final int cursorAfterNext = stringReader.getCursor();
            stringReader.setCursor(cursorBeforeNext);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(stringReader, "#" + tagKey.location()), cursorAfterNext);
          }
          parsedTypeKeys.add(tagKey);
          values.add(Either.right(tagKey));
        } else {
          ResourceLocation identifier = ResourceLocation.read(reader.getReader());
          final int cursorAfterNext = stringReader.getCursor();
          EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElseThrow(() -> {
            stringReader.setCursor(cursorBeforeNext);
            return CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptions.ERROR_ENTITY_TYPE_INVALID.createWithContext(stringReader, identifier.toString()), cursorAfterNext);
          });
          if (parsedTypes.contains(entityType)) {
            stringReader.setCursor(cursorBeforeNext);
            throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(stringReader, entityType.getDescription()), cursorAfterNext);
          }
          parsedTypes.add(entityType);
          values.add(Either.left(entityType));
        }

        stringReader.skipWhitespace();
        // 由于有明确的定界符，因此此处的 skipWhitespace 是安全的。
      }

      reader.addPredicate(new TypesEntityPredicateEntry(values, inverted));
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
   * @see EntitySelectorOptionsMixin#readMultipleGameModes(EntitySelectorParser, Predicate, boolean, GameType)
   */
  @ApiStatus.Internal
  public static boolean mixinReadMultipleGameModes(EntitySelectorParser reader, boolean inverted, @NotNull GameType gameMode) throws CommandSyntaxException {
    final StringReader stringReader = reader.getReader();
    final int cursorBeforeWhite = stringReader.getCursor();
    stringReader.skipWhitespace();

    if (stringReader.canRead() && stringReader.peek() == '|') {
      // 解析更多的游戏模式，例如：
      // m = c <当前cursor> | a | sp

      // 这里在解析时，parsedGameModes 是以 EnumSet 的形式储存的，
      // 但是在序列化之后，可能会被解析为普通的 Set，
      // 两种集的实现方式不同，但效果相同，故不作区分。
      EnumSet<GameType> parsedGameModes = EnumSet.of(gameMode);
      while (stringReader.canRead() && stringReader.peek() == '|') {
        stringReader.skip();
        stringReader.skipWhitespace();
        final int cursorBeforeNext = stringReader.getCursor();

        // 提供游戏模式（包括本模组中提供的 a、1 等名称）的建议。
        reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> SharedSuggestionProvider.suggest(Stream.concat(Arrays.stream(GameType.values()).filter(m -> !parsedGameModes.contains(m)).map(GameType::getName), MixinShared.EXTENDED_GAME_MODE_NAMES.entrySet().stream().filter(entry -> !parsedGameModes.contains(entry.getValue())).map(Map.Entry::getKey)), suggestionsBuilder));

        final String nextString = stringReader.readUnquotedString();
        final GameType next = GameType.byName(nextString, null);
        final int cursorAfterNext = stringReader.getCursor();
        if (next == null) {
          stringReader.setCursor(cursorBeforeNext);
          throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptions.ERROR_GAME_MODE_INVALID.createWithContext(stringReader, nextString), cursorAfterNext);
        } else if (parsedGameModes.contains(next)) {
          stringReader.setCursor(cursorBeforeNext);
          throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(stringReader, nextString), cursorAfterNext);
        } else {
          parsedGameModes.add(next);
        }

        stringReader.skipWhitespace();
        // 由于有明确的定界符，因此此处的 skipWhitespace 是安全的。
      }

      reader.addPredicate(new GameModeEntityPredicateEntry.Multiple(parsedGameModes, inverted));
      return false;
    } else {
      stringReader.setCursor(cursorBeforeWhite);
      return true;
    }
  }

  /**
   * 此方法用于辅助 {@link EntitySelectorOptionsMixin} 中的 mixin。
   */
  @ApiStatus.Internal
  public static void mixinGetScoreSuggestions(EntitySelectorParser entitySelectorReader, StringReader stringReader, @NotNull CommandContext<?> context) {
    if (context.getSource() instanceof final CommandSourceStack serverCommandSource) {
      final int cursor = stringReader.getCursor();
      final ServerScoreboard scoreboard = serverCommandSource.getServer().getScoreboard();
      final Collection<Objective> objectives = scoreboard.getObjectives();
      entitySelectorReader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> SharedSuggestionProvider.suggest(objectives, suggestionsBuilder.createOffset(cursor), Objective::getName, Objective::getDisplayName));
    } else if (context.getSource() instanceof final SharedSuggestionProvider commandSource) {
      entitySelectorReader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.customSuggestion(context));
    }
  }

  /**
   * 此方法用于辅助 {@link EntitySelectorOptionsMixin} 中的 mixin。
   */
  @ApiStatus.Internal
  public static void mixinGetAdvancementIdSuggestions(EntitySelectorParser entitySelectorReader, StringReader stringReader, @NotNull CommandContext<?> context) {
    if (context.getSource() instanceof final CommandSourceStack serverCommandSource) {
      final int cursor = stringReader.getCursor();
      final var advancementLoader = serverCommandSource.getServer().getAdvancements();
      entitySelectorReader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> {
        final SuggestionsBuilder offset = suggestionsBuilder.createOffset(cursor);
        SharedSuggestionProvider.suggestResource(advancementLoader.getAllAdvancements(), offset, AdvancementHolder::id, advancementEntry -> advancementEntry.value().name().orElseGet(() -> TextUtil.literal(advancementEntry.id())));
        return offset.buildFuture();
      });
    } else if (context.getSource() instanceof final SharedSuggestionProvider commandSource) {
      entitySelectorReader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.customSuggestion(context));
    }
  }

  /**
   * 此方法用于辅助 {@link EntitySelectorOptionsMixin} 中的 mixin。
   */
  @ApiStatus.Internal
  public static void mixinGetLootConditionIdSuggestions(EntitySelectorParser entitySelectorReader, StringReader stringReader, @NotNull CommandContext<?> context) {
    if (context.getSource() instanceof final CommandSourceStack serverCommandSource) {
      final int cursor = stringReader.getCursor();
      final var ids = serverCommandSource.getServer().reloadableRegistries().getKeys(Registries.PREDICATE);
      entitySelectorReader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> SharedSuggestionProvider.suggestResource(ids, suggestionsBuilder.createOffset(cursor)));
    } else if (context.getSource() instanceof final SharedSuggestionProvider commandSource) {
      entitySelectorReader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.customSuggestion(context));
    }
  }

  /**
   * 此方法用于辅助 {@link EntitySelectorOptionsMixin} 中的 mixin，返回的谓词仅测试被反向的分数条件。
   */
  @NotNull
  public static Predicate<Entity> mixinInvertedScoredPredicate(List<ScoresEntityPredicateEntry.Entry> invertedScores) {
    return entity -> {
      final MinecraftServer server = entity.getServer();
      if (server == null) {
        return false;
      }
      final Scoreboard scoreboard = server.getScoreboard();
      for (ScoresEntityPredicateEntry.Entry pair : invertedScores) {
        Objective scoreboardObjective = scoreboard.getObjective(pair.name());
        if (scoreboardObjective == null) {
          return false;
        }
        final ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(entity, scoreboardObjective);
        if (score == null) {
          return false;
        }
        int i = score.value();
        if (pair.score().matches(i)) {
          return false;
        }
      }
      return true;
    };
  }

  public static boolean mixinReadLiteralPredicate(EntitySelectorParser reader, boolean inverted, StringReader stringReader) throws CommandSyntaxException {
    reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> {
      final CommandContext<?> context = reader.extension$ec().context;
      if (context != null && context.getSource() instanceof CommandSourceStack source) {
        return SharedSuggestionProvider.suggestResource(source.getServer().reloadableRegistries().getKeys(Registries.PREDICATE), suggestionsBuilder);
      } else if (context != null && context.getSource() instanceof SharedSuggestionProvider commandSource) {
        return commandSource.customSuggestion(context);
      } else {
        return Suggestions.empty();
      }
    });
    if (stringReader.canRead() && stringReader.peek() == '{') {
      reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
      final LootItemCondition lootCondition = ParsingUtil.parseNbt(stringReader, LootItemCondition.DIRECT_CODEC, ModCommandExceptionTypes.INVALID_LOOT_TABLE::create);
      reader.addPredicate(new LootTablePredicateEntityPredicateEntry(new Holder.Direct<>(lootCondition), inverted));
      return true;
    }
    return false;
  }

  @FunctionalInterface
  public interface InapplicableReasonProvider {
    @Nullable
    CommandSyntaxException getReason(EntitySelectorParser reader, String option, int restoreCursor);
  }
}
