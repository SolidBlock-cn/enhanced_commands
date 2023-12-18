package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.SuggestionProvider;

public record StorageNbtDataArgument(Identifier id) implements NbtSourceArgument, NbtTargetArgument {
  public StorageNbtData getNbtData(ServerCommandSource source) {
    return new StorageNbtData(source.getServer().getDataCommandStorage(), id);
  }

  @Override
  public StorageNbtData getNbtSource(ServerCommandSource source) {
    return getNbtData(source);
  }

  @Override
  public StorageNbtData getNbtTarget(ServerCommandSource source) {
    return getNbtData(source);
  }

  public static StorageNbtDataArgument handle(CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final int cursor = parser.reader.getCursor();
    parser.suggestionProviders.add(SuggestionProvider.offset((context, suggestionsBuilder) -> {
      if (context.getSource() instanceof ServerCommandSource source) {
        return CommandSource.suggestIdentifiers(source.getServer().getDataCommandStorage().getIds(), suggestionsBuilder.createOffset(cursor));
      } else if (context.getSource() instanceof CommandSource source) {
        return source.getCompletions(context);
      } else {
        return Suggestions.empty();
      }
    }));
    final Identifier identifier = Identifier.fromCommandInput(parser.reader);
    return new StorageNbtDataArgument(identifier);
  }
}
