package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.Parser;

/**
 * @see net.minecraft.command.argument.PosArgument
 */
public interface RegionArgument {
  @NotNull
  static RegionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final int cursorOnStart = parser.reader.getCursor();
    for (Parser<RegionArgument> argumentParser : RegionTypes.PARSERS) {
      parser.reader.setCursor(cursorOnStart);
      final RegionArgument parse = argumentParser.parse(commandRegistryAccess, parser, suggestionsOnly, true);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    parser.reader.setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
  }

  Region toAbsoluteRegion(ServerCommandSource source);
}
