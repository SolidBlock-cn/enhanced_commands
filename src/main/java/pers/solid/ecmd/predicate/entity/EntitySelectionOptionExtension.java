package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.text.Text;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EntitySelectionOptionExtension {
  /**
   * 此映射用于对 {@link net.minecraft.command.EntitySelectorOptions#getHandler(EntitySelectorReader, String, int)} 中遇到不要应用的选项时进行扩展，如果遇到了不可应用的选项，会尝试从此映射中查找不可应用的原因，而非简单的某选项不适用的消息。如果为 {@code null}，则不影响原版的行为。
   */
  public static final Map<String, Function<EntitySelectorReader, @Nullable CommandSyntaxException>> INAPPLICABLE_REASONS = new HashMap<>();

  public static final DynamicCommandExceptionType DUPLICATE_OPTION = new DynamicCommandExceptionType(optionName -> Text.translatable("enhanced_commands.argument.entity.options.duplicate_option", optionName));
  public static final SimpleCommandExceptionType MIXED_NAME = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_name"));
  public static final SimpleCommandExceptionType INVALID_LIMIT_FOR_AT_S = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_limit_for_@s"));
  public static final SimpleCommandExceptionType INVALID_SORT_FOR_AT_S = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.invalid_sort_for_@s"));
  public static final SimpleCommandExceptionType MIXED_GAME_MODE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_game_mode"));
  public static final SimpleCommandExceptionType MIXED_TYPE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.entity.options.mixed_type"));
  public static final DynamicCommandExceptionType INVALID_TYPE_FOR_SELECTOR = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.entity.options.invalid_type_for_selector", o));


  private static void registerInapplicableReasons() {
    final var map = INAPPLICABLE_REASONS;
    ensureSingle("name");
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
    map.put("limit", reader -> reader.isSenderOnly() ? INVALID_LIMIT_FOR_AT_S.createWithContext(reader.getReader()) : DUPLICATE_OPTION.createWithContext(reader.getReader(), "limit"));
    map.put("sort", reader -> reader.isSenderOnly() ? INVALID_SORT_FOR_AT_S.createWithContext(reader.getReader()) : DUPLICATE_OPTION.createWithContext(reader.getReader(), "sort"));
    ensureSingle("gamemode");
    ensureSingle("team");
    map.put("type", reader -> {
      if (reader.selectsEntityType()) {
        final EntitySelectorReaderExtras entitySelectorReaderExtras = EntitySelectorReaderExtras.getOf(reader);
        final String atVariable = entitySelectorReaderExtras.atVariable;
        if (atVariable != null) {
          switch (atVariable) {
            case "a", "r", "p" -> {
              return INVALID_TYPE_FOR_SELECTOR.createWithContext(reader.getReader(), "@" + atVariable);
            }
          }
        }
        return DUPLICATE_OPTION.createWithContext(reader.getReader(), "type");
      }
      return null;
    });
  }

  private static void ensureSingle(String optionName) {
    INAPPLICABLE_REASONS.put(optionName, reader -> DUPLICATE_OPTION.createWithContext(reader.getReader(), optionName));
  }

  public static void init() {
    registerInapplicableReasons();
    Validate.notEmpty(INAPPLICABLE_REASONS);
  }
}
