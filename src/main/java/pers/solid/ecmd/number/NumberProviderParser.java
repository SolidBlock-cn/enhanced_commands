package pers.solid.ecmd.number;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import pers.solid.ecmd.parse.FunctionsParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;

public final class NumberProviderParser {
  public static final SimpleCommandExceptionType MISSING_MIN = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.numer_provider.missing_min"));
  public static final SimpleCommandExceptionType MISSING_MAX = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.numer_provider.missing_max"));
  public static final FunctionsParser<NumberProvider> FUNCTIONS_PARSER = FunctionsParser.create();

  private NumberProviderParser() {
  }

  public static <S> NumberProvider parseDirectNumbers(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorStart = reader.getCursor();
    final BridgeFloatRange parse = BridgeFloatRange.parse(reader);
    if (parse.min == null) {
      final int cursorEnd = reader.getCursor();
      reader.setCursor(cursorStart);
      throw EnhancedCommandSyntaxException.withCursorEnd(MISSING_MIN.createWithContext(reader), cursorEnd);
    } else if (parse.max == null) {
      final int cursorEnd = reader.getCursor();
      reader.setCursor(cursorStart);
      throw EnhancedCommandSyntaxException.withCursorEnd(MISSING_MAX.createWithContext(reader), cursorEnd);
    } else {
      if (parse.isExact()) {
        return ConstantValue.exactly(parse.min);
      } else
        return UniformGenerator.between(parse.min, parse.max);
    }
  }

  public static <S> NumberProvider parseDirectNbt(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    return ParsingUtil.parseNbt(reader, NumberProviders.CODEC, s -> EnhancedCommandsCommandExceptionTypes.CANNOT_PARSE.createWithContext(reader, s));
  }

  public static <S> NumberProvider parse(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBefore = reader.getCursor();
    final NumberProvider parse = FUNCTIONS_PARSER.parse(parseContext);
    if (parse != null) {
      return parse;
    } else {
      reader.setCursor(cursorBefore);
    }
    if (reader.canRead()) {
      final char peek = reader.peek();
      if (peek == '{' || peek == '"' || peek == '\'') {
        return parseDirectNbt(parseContext);
      }
    }
    return parseDirectNumbers(parseContext);
  }

  public static void registerFunctions() {
    final var functionsParser = FUNCTIONS_PARSER;
    functionsParser.register("constant", Component.translatable("enhanced_commands.argument.numer_provider.constant"), ConstantFunctionContentParser::new);
    functionsParser.register("binomial", Component.translatable("enhanced_commands.argument.numer_provider.binomial"), BinomialFunctionContentParser::new);
    functionsParser.register("uniform", Component.translatable("enhanced_commands.argument.numer_provider.uniform"), UniformFunctionContentParser::new);
  }
}
