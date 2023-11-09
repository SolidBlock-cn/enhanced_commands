package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.mixin.command.EntitySelectorOptionsAccessor;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.predicate.NumberRange;
import net.minecraft.text.Text;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.HashMap;
import java.util.Map;

public class EntitySelectionOptionExtension {
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
    EntitySelectorOptionsAccessor.callPutOption("r", reader -> {
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
        }
        reader.setDistance(NumberRange.FloatRange.between(original.getMin(), value));
      }
    }, reader -> reader.getDistance().isDummy() || EntitySelectorReaderExtras.getOf(reader).implicitDistance && reader.getDistance().getMax() != null, Text.translatable("enhanced_commands.argument.entity.options.r.description"));
    EntitySelectorOptionsAccessor.callPutOption("rm", reader -> {
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
        }
        reader.setDistance(NumberRange.FloatRange.between(value, original.getMax()));
      }
    }, reader -> reader.getDistance().isDummy() || EntitySelectorReaderExtras.getOf(reader).implicitDistance && reader.getDistance().getMax() != null, Text.translatable("enhanced_commands.argument.entity.options.rm.description"));
  }

  public static void init() {
    registerInapplicableReasons();
    registerOptionAliases();
    registerModOptions();
    Validate.notEmpty(INAPPLICABLE_REASONS);
    Validate.notEmpty(OPTION_NAME_ALIASES);
  }

  @FunctionalInterface
  public interface InapplicableReasonProvider {
    @Nullable CommandSyntaxException getReason(EntitySelectorReader reader, String option, int restoreCursor);
  }
}
