package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import net.minecraft.util.parsing.packrat.commands.ResourceLocationParseRule;
import pers.solid.ecmd.util.DefaultNamespace;

import java.util.Optional;

/**
 * 类似 {@link ResourceLocationParseRule}，但是解析时会采用不同于原版的默认命名空间。
 */
public record IdWithDefaultNamespaceParseRule(DefaultNamespace defaultNamespace) implements Rule<StringReader, ResourceLocation> {
  public static final IdWithDefaultNamespaceParseRule ENHANCED_COMMANDS = new IdWithDefaultNamespaceParseRule(DefaultNamespace.ENHANCED_COMMANDS);

  @Override
  public Optional<ResourceLocation> parse(ParseState<StringReader> parseState) {
    parseState.input().skipWhitespace();

    try {
      return Optional.of(this.defaultNamespace.fromStringReader(parseState.input()));
    } catch (CommandSyntaxException var3) {
      return Optional.empty();
    }
  }
}
