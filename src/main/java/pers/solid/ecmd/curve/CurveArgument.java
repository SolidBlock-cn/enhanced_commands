package pers.solid.ecmd.curve;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;

public interface CurveArgument<T extends Curve> {
  @NotNull
  static CurveArgument<?> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final int cursorOnStart = parser.reader.getCursor();/*
    final Stream<CurveType<?>> stream = commandRegistryAccess.createWrapper(CurveType.REGISTRY_KEY).streamEntries().map(RegistryEntry.Reference::value);
    for (CurveType<?> type : (Iterable<CurveType<?>>) stream::iterator) {
      parser.reader.setCursor(cursorOnStart);
      final CurveArgument<?> parse = type.parse(commandRegistryAccess, parser, suggestionsOnly);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }*/
    parser.reader.setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
  }

  T toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException;
}
