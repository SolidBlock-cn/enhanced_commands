package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;

/**
 * @see net.minecraft.command.argument.PosArgument
 */
public interface RegionArgument {
  @NotNull
  static RegionArgument parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final SuggestedParser<?> parser = parseContext.parser();
    final int cursorOnStart = parser.reader.getCursor();
    for (Parser<RegionArgument> argumentParser : RegionTypes.PARSERS) {
      parser.reader.setCursor(cursorOnStart);
      final RegionArgument parse = argumentParser.parse(parseContext.withAllowSparse(true));
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    parser.reader.setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
  }

  Region toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException;
}
