package pers.solid.ecmd.curve;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.parse.ParseContext;

public interface CurveArgument<T extends Curve> {
  @NotNull
  static CurveArgument<?> parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final int cursorOnStart = parseContext.reader().getCursor();/*
    final Stream<CurveType<?>> stream = registryAccess.createWrapper(CurveType.REGISTRY_KEY).streamEntries().map(RegistryEntry.Reference::probability);
    for (CurveType<?> type : (Iterable<CurveType<?>>) stream::iterator) {
      reader.setCursor(cursorOnStart);
      final CurveArgument<?> parse = type.parse(registryAccess, parser, suggestionsOnly);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }*/
    parseContext.reader().setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parseContext.reader());
  }

  T toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException;
}
