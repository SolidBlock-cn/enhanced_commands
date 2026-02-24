package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 表示正则表达式的参数类型。其在解析时会解析成正则表达式的形式，如果正则表达式无效，将无法解析。
 */
public enum RegexArgument implements ArgumentType<Pattern> {
  REGEX;

  private static final Collection<String> EXAMPLES = List.of("text", "\\(.*?\\)", "[A-Za-z0-9]+");

  @Override
  public Pattern parse(StringReader reader) throws CommandSyntaxException {
    return ParsingUtil.readRegex(reader);
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }

  public static Pattern getRegex(CommandContext<?> context, String name) {
    return context.getArgument(name, Pattern.class);
  }
}
