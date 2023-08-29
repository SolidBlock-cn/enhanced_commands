package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;

import java.util.stream.Stream;

/**
 * @see net.minecraft.command.argument.PosArgument
 */
public interface RegionArgument<T extends Region> {
  @NotNull
  static RegionArgument<?> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final int cursorOnStart = parser.reader.getCursor();
    final Stream<RegionType<?>> stream = commandRegistryAccess.createWrapper(RegionType.REGISTRY_KEY).streamEntries().map(RegistryEntry.Reference::value);
    for (RegionType<?> type : (Iterable<RegionType<?>>) stream::iterator) {
      parser.reader.setCursor(cursorOnStart);
      final RegionArgument<?> parse = type.parse(commandRegistryAccess, parser, suggestionsOnly);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    parser.reader.setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
  }

  T toAbsoluteRegion(ServerCommandSource source);
}
