package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;

/**
 * @see net.minecraft.command.argument.PosArgument
 */
public interface RegionArgument<T extends Region> {
  @NotNull
  static RegionArgument<?> parse(SuggestedParser parser) throws CommandSyntaxException {
    CommandSyntaxException exception = null;
    final int cursorOnStart = parser.reader.getCursor();
    int cursorOnEnd = cursorOnStart;
    for (RegionType<?> type : RegionType.REGISTRY) {
      try {
        parser.reader.setCursor(cursorOnStart);
        final RegionArgument<?> parse = type.parse(parser);
        if (parse != null) {
          // keep the current position of the cursor
          return parse;
        }

      } catch (CommandSyntaxException exception1) {
        cursorOnEnd = parser.reader.getCursor();
        exception = exception1;
      }
    }
    parser.reader.setCursor(cursorOnEnd);
    if (exception != null) throw exception;
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
  }

  T toAbsoluteRegion(ServerCommandSource source);
}
