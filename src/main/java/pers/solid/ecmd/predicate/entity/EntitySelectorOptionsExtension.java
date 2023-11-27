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
import net.fabricmc.fabric.mixin.command.EntitySelectorOptionsAccessor;
import net.minecraft.advancement.Advancement;
import net.minecraft.command.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.mixin.EntitySelectorOptionsMixin;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;

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
  public static final SimpleCommandExceptionType MIXED_NAME = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_name"));
  public static final SimpleCommandExceptionType INVALID_LIMIT_FOR_AT_S = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_limit_for_@s"));
  public static final SimpleCommandExceptionType INVALID_SORT_FOR_AT_S = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_sort_for_@s"));
  public static final SimpleCommandExceptionType MIXED_GAME_MODE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_game_mode"));
  public static final SimpleCommandExceptionType MIXED_TYPE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_type"));
  public static final DynamicCommandExceptionType INVALID_TYPE_FOR_SELECTOR = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.entity.options.invalid_type_for_selector", o));
  public static final DynamicCommandExceptionType DISTANCE_ALREADY_EXPLICIT = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.entity.options.distance_already_explicit", o));
  public static final SimpleCommandExceptionType DISTANCE_ALREADY_IMPLICIT = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.distance_already_implicit"));
  public static final SimpleCommandExceptionType INVALID_NEGATIVE_LIMIT_WITH_SORTER = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_negative_limit_with_sorter"));
  public static final SimpleCommandExceptionType INVALID_SORTER_WITH_NEGATIVE_LIMIT = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_sorter_with_negative_limit"));

  private static void registerInapplicableReasons() {
    final var map = INAPPLICABLE_REASONS;
    map.put("name", (reader, option, restoreCursor) -> {
      final StringReader stringReader = reader.getReader();
      stringReader.skipWhitespace();
      if (stringReader.canRead() && stringReader.read() == EntitySelectorReader.ARGUMENT_DEFINER == reader.readNegationCharacter()) {
        stringReader.setCursor(restoreCursor);
        return MIXED_NAME.createWithContext(stringReader);
      } else {
        stringReader.setCursor(restoreCursor);
        return DUPLICATE_OPTION.createWithContext(stringReader, option);
      }
    });
    map.put("distance", (reader, option, restoreCursor) -> {
      reader.getReader().setCursor(restoreCursor);
      if (EntitySelectorReaderExtras.getOf(reader).implicitDistance) {
        return DISTANCE_ALREADY_IMPLICIT.createWithContext(reader.getReader());
      } else {
        return DUPLICATE_OPTION.createWithContext(reader.getReader(), option);
      }
    });
    ensureSingle("level");
    ensureSingle("x");
    ensureSingle("y");
    ensureSingle("z");
    ensureSingle("dx");
    ensureSingle("dy");
    ensureSingle("dz");
    ensureSingle("x_rotation");
    ensureSingle("y_rotation");
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
    map.put("gamemode", (reader, option, restoreCursor) -> {
      final StringReader stringReader = reader.getReader();
      stringReader.skipWhitespace();
      if (stringReader.canRead() && stringReader.read() == EntitySelectorReader.ARGUMENT_DEFINER == reader.readNegationCharacter()) {
        stringReader.setCursor(restoreCursor);
        return MIXED_GAME_MODE.createWithContext(stringReader);
      } else {
        stringReader.setCursor(restoreCursor);
        return DUPLICATE_OPTION.createWithContext(stringReader, option);
      }
    });
    ensureSingle("team");
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
          return MIXED_TYPE.createWithContext(stringReader);
        } else {
          stringReader.setCursor(restoreCursor);
          return DUPLICATE_OPTION.createWithContext(stringReader, option);
        }
      }
      return null;
    });
    ensureSingle("scores");
    ensureSingle("advancements");
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

  private static void ensureSingle(String optionName) {
    INAPPLICABLE_REASONS.put(optionName, (reader, option, restoreCursor) -> {
      reader.getReader().setCursor(restoreCursor);
      return DUPLICATE_OPTION.createWithContext(reader.getReader(), option);
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
      final CommandRegistryAccess registryAccess = MixinSharedVariables.getCommandRegistryAccess();
      final RegionArgument<?> regionArgument = RegionArgument.parse(registryAccess, parser, false);

      EntitySelectorReaderExtras.getOf(reader).addFunction(source -> {
        final Region region = regionArgument.toAbsoluteRegion(source);
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
        final var predicate = Predicates.or(ImmutableList.copyOf(Lists.transform(build, input -> SelectorEntityPredicate.asPredicate(input, source))));
        return inverted ? Predicates.not(predicate) : predicate;
      });
      EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new AlternativesEntityPredicateEntry(build, source, inverted));
    }, Predicates.alwaysTrue(), Text.translatable("enhanced_commands.argument.entity.options.alternatives"));

    putOption("health", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.readNegationCharacter();
      final int cursorBefore = stringReader.getCursor();
      reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> ParsingUtil.suggestString("max", suggestionsBuilder).buildFuture());
      final String unquotedString = stringReader.readUnquotedString();
      if ("max".equals(unquotedString)) {
        reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
        reader.setPredicate(entity -> entity instanceof LivingEntity livingEntity && (livingEntity.getHealth() == livingEntity.getMaxHealth()) != inverted);
        EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new HealthMaxEntityPredicateEntry(inverted));
      } else {
        stringReader.setCursor(cursorBefore);
        final NumberRange.FloatRange floatRange = NumberRange.FloatRange.parse(stringReader);
        reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
        reader.setPredicate(entity -> entity instanceof LivingEntity livingEntity && floatRange.test(livingEntity.getHealth()) != inverted);
        EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new HealthEntityPredicateEntry(floatRange, inverted));
      }
    }, Predicates.alwaysTrue(), Text.translatable("enhanced_commands.argument.entity.options.health"));

    putOption("fire", reader -> {
      final StringReader stringReader = reader.getReader();
      final boolean inverted = reader.readNegationCharacter();
      final NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
      reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
      reader.setPredicate(entity -> intRange.test(entity.getFireTicks()) != inverted);
      EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new FireEntityPredicateEntry(intRange, inverted));
    }, Predicates.alwaysTrue(), Text.translatable("enhanced_commands.argument.entity.options.fire"));
  }

  private static void putOption(String id, EntitySelectorOptions.SelectorHandler handler, Predicate<EntitySelectorReader> condition, Text description) {
    EntitySelectorOptionsAccessor.callPutOption(id, handler, condition, description);
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
        reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> CommandSource.suggestMatching(Stream.concat(Arrays.stream(GameMode.values()).filter(m -> !parsedGameModes.contains(m)).map(GameMode::getName), MixinSharedVariables.EXTENDED_GAME_MODE_NAMES.entrySet().stream().filter(entry -> !parsedGameModes.contains(entry.getValue())).map(Map.Entry::getKey)), suggestionsBuilder));

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
        String remainingLowerCase = offset.getRemainingLowerCase();
        CommandSource.forEachMatching(advancementLoader.getAdvancements(), remainingLowerCase, Advancement::getId, advancement -> offset.suggest(advancement.getId().toString(), advancement.toHoverableText()));
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
      entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> CommandSource.suggestMatching(predicateManager.getIds().stream().map(Identifier::toString), suggestionsBuilder.createOffset(cursor)));
    } else if (context.getSource() instanceof final CommandSource commandSource) {
      entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.getCompletions(context));
    }
  }

  @FunctionalInterface
  public interface InapplicableReasonProvider {
    @Nullable CommandSyntaxException getReason(EntitySelectorReader reader, String option, int restoreCursor);
  }
}
