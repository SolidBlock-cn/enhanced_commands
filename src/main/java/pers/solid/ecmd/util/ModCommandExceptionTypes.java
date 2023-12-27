package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.*;
import net.minecraft.command.EntitySelectorOptions;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;

public final class ModCommandExceptionTypes {
  public static final DynamicCommandExceptionType INVALID_REGEX = new DynamicCommandExceptionType(msg -> Text.translatable("enhanced_commands.argument.regex.invalid", msg));
  public static final Dynamic2CommandExceptionType BLOCK_ID_FEATURE_FLAG_REQUIRED = new Dynamic2CommandExceptionType((blockId, blockName) -> Text.translatable("enhanced_commands.argument.block.feature_required", blockId, blockName));
  public static final Dynamic2CommandExceptionType ITEM_ID_FEATURE_FLAG_REQUIRED = new Dynamic2CommandExceptionType((itemId, itemName) -> Text.translatable("enhanced_commands.argument.item.feature_required", itemId, itemName));
  public static final Dynamic2CommandExceptionType ENTITY_TYPE_ID_FEATURE_FLAG_REQUIRED = new Dynamic2CommandExceptionType((itemId, itemName) -> Text.translatable("enhanced_commands.argument.entity_type.feature_required", itemId, itemName));
  public static final Dynamic2CommandExceptionType BIOME_ID_FEATURE_FLAG_REQUIRED = new Dynamic2CommandExceptionType((biomeId, biomeName) -> Text.translatable("enhanced_commands.argument.biome.feature_required", biomeId, biomeName));
  public static final Dynamic2CommandExceptionType EXPECTED_2_SYMBOLS = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.parsing.expected.2", a, b));
  public static final Dynamic3CommandExceptionType EXPECTED_3_SYMBOLS = new Dynamic3CommandExceptionType((a, b, c) -> Text.translatable("enhanced_commands.parsing.expected.3", a, b, c));
  public static final Dynamic4CommandExceptionType EXPECTED_4_SYMBOLS = new Dynamic4CommandExceptionType((a, b, c, d) -> Text.translatable("enhanced_commands.parsing.expected.4", a, b, c, d));
  public static final DynamicCommandExceptionType UNKNOWN_KEYWORD = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.parsing.unknown_keyword", o));
  public static final DynamicCommandExceptionType UNKNOWN_FUNCTION = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.parsing.unknown_function", o));
  public static final DynamicCommandExceptionType DUPLICATE_KEYWORD = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.parsing.duplicate_keyword", o));
  public static final DynamicCommandExceptionType DUPLICATE_VALUE = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.parsing.duplicate_value", o));
  private static final Text VALID_UNITS = Text.translatable("enhanced_commands.parsing.angle_accepted_values");
  public static final DynamicCommandExceptionType ANGLE_UNIT_EXPECTED = new DynamicCommandExceptionType(number -> Text.translatable("enhanced_commands.parsing.angle_unit_expected", number, VALID_UNITS));
  public static final DynamicCommandExceptionType ANGLE_UNIT_UNKNOWN = new DynamicCommandExceptionType(actual -> Text.translatable("enhanced_commands.parsing.angle_unit_unknown", actual, VALID_UNITS));
  public static final DynamicCommandExceptionType CANNOT_PARSE = new DynamicCommandExceptionType(reason -> Text.translatable("enhanced_commands.parsing.cannot_parse", reason));
  public static final DynamicCommandExceptionType MALFORMED_JSON = new DynamicCommandExceptionType(reason -> Text.translatable("enhanced_commands.parsing.malformed_json", reason));
  public static final DynamicCommandExceptionType UNKNOWN_LOOT_TABLE_PREDICATE_ID = new DynamicCommandExceptionType(reason -> Text.translatable("enhanced_commands.parsing.unknown_loot_table_predicate", reason));
  public static final DynamicCommandExceptionType INVALID_LOOT_TABLE_JSON = new DynamicCommandExceptionType(reason -> Text.translatable("enhanced_commands.parsing.invalid_loot_table_json", reason));
  public static final SimpleCommandExceptionType EXPECTED_WHITESPACE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.parsing.expected_whitespace"));

  public static final DynamicCommandExceptionType UNKNOWN_STATUS_EFFECT = new DynamicCommandExceptionType(id -> Text.translatable("enhanced_commands.parsing.unknown_registry_entry.effect", id));
  public static final DynamicCommandExceptionType UNKNOWN_BIOME = new DynamicCommandExceptionType(id -> Text.translatable("enhanced_commands.parsing.unknown_registry_entry.biome", id));

  public static final Map<RegistryKey<? extends Registry<?>>, DynamicCommandExceptionType> REGISTRY_ENTRY_EXCEPTION_TYPES = Util.make(new HashMap<>(), map -> {
    map.put(RegistryKeys.BLOCK, BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION);
    map.put(RegistryKeys.ITEM, new DynamicCommandExceptionType(id -> Text.translatable("argument.item.id.invalid", id)));
    map.put(RegistryKeys.BIOME, UNKNOWN_BIOME);
    map.put(RegistryKeys.ENTITY_TYPE, EntitySelectorOptions.INVALID_TYPE_EXCEPTION);
    map.put(RegistryKeys.STATUS_EFFECT, UNKNOWN_STATUS_EFFECT);
  });

  private ModCommandExceptionTypes() {
  }
}
