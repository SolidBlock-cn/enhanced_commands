package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.text.Text;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EntitySelectionOptionExtension {
  /**
   * 此映射用于对 {@link net.minecraft.command.EntitySelectorOptions#getHandler(EntitySelectorReader, String, int)} 中遇到不要应用的选项时进行扩展，如果遇到了不可应用的选项，会尝试从此映射中查找不可应用的原因，而非简单的某选项不适用的消息。如果为 {@code null}，则不影响原版的行为。
   */
  public static final Map<String, InapplicableReasonProvider> INAPPLICABLE_REASONS = new HashMap<>();

  @FunctionalInterface
  public interface InapplicableReasonProvider {
    @Nullable CommandSyntaxException getReason(EntitySelectorReader reader, String option, int restoreCursor);
  }

  public static final DynamicCommandExceptionType DUPLICATE_OPTION = new DynamicCommandExceptionType(optionName -> Text.translatable("enhanced_commands.argument.entity.options.duplicate_option", optionName));
  public static final SimpleCommandExceptionType MIXED_NAME = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_name"));
  public static final SimpleCommandExceptionType INVALID_LIMIT_FOR_AT_S = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_limit_for_@s"));
  public static final SimpleCommandExceptionType INVALID_SORT_FOR_AT_S = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_sort_for_@s"));
  public static final SimpleCommandExceptionType MIXED_GAME_MODE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_game_mode"));
  public static final SimpleCommandExceptionType MIXED_TYPE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_type"));
  public static final DynamicCommandExceptionType INVALID_TYPE_FOR_SELECTOR = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.entity.options.invalid_type_for_selector", o));


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
    ensureSingle("distance");
    ensureSingle("level");
    ensureSingle("x");
    ensureSingle("y");
    ensureSingle("z");
    ensureSingle("dx");
    ensureSingle("dy");
    ensureSingle("dz");
    ensureSingle("x_rotation");
    ensureSingle("y_rotation");
    map.put("limit", (reader, option, restoreCursor) -> reader.isSenderOnly() ? INVALID_LIMIT_FOR_AT_S.createWithContext(reader.getReader()) : DUPLICATE_OPTION.createWithContext(reader.getReader(), "limit"));
    map.put("sort", (reader, option, restoreCursor) -> reader.isSenderOnly() ? INVALID_SORT_FOR_AT_S.createWithContext(reader.getReader()) : DUPLICATE_OPTION.createWithContext(reader.getReader(), "sort"));
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
  }

  private static void ensureSingle(String optionName) {
    INAPPLICABLE_REASONS.put(optionName, (reader, option, restoreCursor) -> {
      reader.getReader().setCursor(restoreCursor);
      return DUPLICATE_OPTION.createWithContext(reader.getReader(), option);
    });
  }

  public static void init() {
    registerInapplicableReasons();
    Validate.notEmpty(INAPPLICABLE_REASONS);
  }
}
