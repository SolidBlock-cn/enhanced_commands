package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.*;
import net.minecraft.Util;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;

/**
 * 此模组包含的一些扩展的 {@link CommandExceptionType}。
 */
public final class ModCommandExceptionTypes {
  public static final DynamicCommandExceptionType INVALID_REGEX = new DynamicCommandExceptionType(msg -> Component.translatable("enhanced_commands.argument.regex.invalid", msg));
  public static final Dynamic2CommandExceptionType BLOCK_ID_FEATURE_FLAG_REQUIRED = new Dynamic2CommandExceptionType((blockId, blockName) -> Component.translatable("enhanced_commands.argument.block.feature_required", blockId, blockName));
  public static final Dynamic2CommandExceptionType ITEM_ID_FEATURE_FLAG_REQUIRED = new Dynamic2CommandExceptionType((itemId, itemName) -> Component.translatable("enhanced_commands.argument.item.feature_required", itemId, itemName));
  public static final Dynamic2CommandExceptionType ENTITY_TYPE_ID_FEATURE_FLAG_REQUIRED = new Dynamic2CommandExceptionType((itemId, itemName) -> Component.translatable("enhanced_commands.argument.entity_type.feature_required", itemId, itemName));
  public static final Dynamic2CommandExceptionType BIOME_ID_FEATURE_FLAG_REQUIRED = new Dynamic2CommandExceptionType((biomeId, biomeName) -> Component.translatable("enhanced_commands.argument.biome.feature_required", biomeId, biomeName));
  public static final Dynamic2CommandExceptionType EXPECTED_2_SYMBOLS = new Dynamic2CommandExceptionType((a, b) -> Component.translatableEscape("enhanced_commands.parsing.expected.2", a, b));
  public static final Dynamic3CommandExceptionType EXPECTED_3_SYMBOLS = new Dynamic3CommandExceptionType((a, b, c) -> Component.translatableEscape("enhanced_commands.parsing.expected.3", a, b, c));
  public static final Dynamic4CommandExceptionType EXPECTED_4_SYMBOLS = new Dynamic4CommandExceptionType((a, b, c, d) -> Component.translatableEscape("enhanced_commands.parsing.expected.4", a, b, c, d));
  public static final DynamicCommandExceptionType UNKNOWN_KEYWORD = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.parsing.unknown_keyword", o));
  public static final DynamicCommandExceptionType UNKNOWN_FUNCTION = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.parsing.unknown_function", o));
  public static final DynamicCommandExceptionType DUPLICATE_KEYWORD = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.parsing.duplicate_keyword", o));
  public static final DynamicCommandExceptionType DUPLICATE_VALUE = new DynamicCommandExceptionType(o -> Component.translatableEscape("enhanced_commands.parsing.duplicate_value", o));
  private static final Component VALID_UNITS = Component.translatable("enhanced_commands.parsing.angle_accepted_values");
  public static final DynamicCommandExceptionType ANGLE_UNIT_EXPECTED = new DynamicCommandExceptionType(number -> Component.translatable("enhanced_commands.parsing.angle_unit_expected", number, VALID_UNITS));
  public static final DynamicCommandExceptionType ANGLE_UNIT_UNKNOWN = new DynamicCommandExceptionType(actual -> Component.translatable("enhanced_commands.parsing.angle_unit_unknown", actual, VALID_UNITS));
  public static final DynamicCommandExceptionType CANNOT_PARSE = new DynamicCommandExceptionType(reason -> Component.translatable("enhanced_commands.parsing.cannot_parse", reason));
  public static final DynamicCommandExceptionType MALFORMED_JSON = new DynamicCommandExceptionType(reason -> Component.translatable("enhanced_commands.parsing.malformed_json", reason));
  public static final DynamicCommandExceptionType UNKNOWN_LOOT_TABLE_PREDICATE_ID = new DynamicCommandExceptionType(reason -> Component.translatable("enhanced_commands.parsing.unknown_loot_table_predicate", reason));
  public static final DynamicCommandExceptionType UNKNOWN_BLOCK_FUNCTION_ID = new DynamicCommandExceptionType(reason -> Component.translatable("enhanced_commands.block_function.reference.unknown_id", reason));
  public static final DynamicCommandExceptionType UNKNOWN_BLOCK_PREDICATE_ID = new DynamicCommandExceptionType(reason -> Component.translatable("enhanced_commands.block_predicate.reference.unknown_id", reason));
  public static final DynamicCommandExceptionType INVALID_LOOT_TABLE = new DynamicCommandExceptionType(reason -> Component.translatable("enhanced_commands.parsing.invalid_loot_table", reason));
  public static final SimpleCommandExceptionType EXPECTED_WHITESPACE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.parsing.expected_whitespace"));

  public static final DynamicCommandExceptionType UNKNOWN_STATUS_EFFECT = new DynamicCommandExceptionType(id -> Component.translatable("enhanced_commands.parsing.unknown_registry_entry.effect", id));
  public static final DynamicCommandExceptionType UNKNOWN_BIOME = new DynamicCommandExceptionType(id -> Component.translatable("enhanced_commands.parsing.unknown_registry_entry.biome", id));
  public static final SimpleCommandExceptionType CONTAINS_UPPER_CASE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.id.contains_upper_case"));

  public static final Map<ResourceKey<? extends Registry<?>>, DynamicCommandExceptionType> REGISTRY_ENTRY_EXCEPTION_TYPES = Util.make(new HashMap<>(), map -> {
    map.put(Registries.BLOCK, BlockStateParser.ERROR_UNKNOWN_BLOCK);
    map.put(Registries.ITEM, new DynamicCommandExceptionType(id -> Component.translatable("argument.item.id.invalid", id)));
    map.put(Registries.BIOME, UNKNOWN_BIOME);
    map.put(Registries.ENTITY_TYPE, EntitySelectorOptions.ERROR_ENTITY_TYPE_INVALID);
    map.put(Registries.MOB_EFFECT, UNKNOWN_STATUS_EFFECT);
  });

  private ModCommandExceptionTypes() {
  }
}
