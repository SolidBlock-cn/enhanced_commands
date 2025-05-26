package pers.solid.ecmd.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record StorageNbtDataArgument(Identifier id) implements NbtSourceArgument<Identifier>, NbtTargetArgument<Identifier> {
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

  public static StorageNbtDataArgument handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final int cursor = reader.getCursor();
    parseContext.setSuggestion((context, suggestionsBuilder) -> {
      if (context.getSource() instanceof ServerCommandSource source) {
        return CommandSource.suggestIdentifiers(source.getServer().getDataCommandStorage().getIds(), suggestionsBuilder.createOffset(cursor));
      } else if (context.getSource() instanceof CommandSource source) {
        return source.getCompletions(context);
      } else {
        return Suggestions.empty();
      }
    });
    final Identifier identifier = Identifier.fromCommandInput(reader);
    return new StorageNbtDataArgument(identifier);
  }
}
