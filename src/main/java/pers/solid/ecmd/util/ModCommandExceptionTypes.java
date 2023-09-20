package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.text.Text;

public final class ModCommandExceptionTypes {
  public static final DynamicCommandExceptionType INVALID_REGEX = new DynamicCommandExceptionType(msg -> Text.translatable("enhancedCommands.argument.regex.invalid", msg));
  public static final Dynamic2CommandExceptionType FEATURE_REQUIRED = new Dynamic2CommandExceptionType((blockId, blockName) -> Text.translatable("enhancedCommands.argument.block.feature_required", blockId, blockName));
  public static final Dynamic2CommandExceptionType EXPECTED_2_SYMBOLS = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.parsing.expected.2", a, b));
  public static final Dynamic3CommandExceptionType EXPECTED_3_SYMBOLS = new Dynamic3CommandExceptionType((a, b, c) -> Text.translatable("enhancedCommands.parsing.expected.2", a, b, c));
  public static final Dynamic4CommandExceptionType EXPECTED_4_SYMBOLS = new Dynamic4CommandExceptionType((a, b, c, d) -> Text.translatable("enhancedCommands.parsing.expected.2", a, b, c, d));
  public static final DynamicCommandExceptionType UNKNOWN_KEYWORD = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.parsing.unknown_keyword", o));
  public static final DynamicCommandExceptionType DUPLICATE_KEYWORD = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.parsing.duplicate_keyword", o));
  private static final Text VALID_UNITS = Text.translatable("enhancedCommands.parsing.angle_accepted_values");
  public static final DynamicCommandExceptionType ANGLE_UNIT_EXPECTED = new DynamicCommandExceptionType(number -> Text.translatable("enhancedCommands.parsing.angle_unit_expected", number, VALID_UNITS));
  public static final DynamicCommandExceptionType ANGLE_UNIT_UNKNOWN = new DynamicCommandExceptionType(actual -> Text.translatable("enhancedCommands.parsing.angle_unit_unknown", actual, VALID_UNITS));

  private ModCommandExceptionTypes() {
  }
}
