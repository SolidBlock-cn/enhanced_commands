package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.nbt.NbtDataRegistry;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.nbt.NbtSourceArgument;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record NbtSourceArgumentType(CommandRegistryAccess registryAccess) implements ArgumentType<NbtSourceArgument> {
  public static NbtSourceArgumentType nbtSource(CommandRegistryAccess registryAccess) {
    return new NbtSourceArgumentType(registryAccess);
  }

  public static NbtSourceArgument getNbtSourceArgument(CommandContext<?> context, String name) {
    return context.getArgument(name, NbtSourceArgument.class);
  }

  public static NbtSource getNbtSource(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
    return getNbtSourceArgument(context, name).getNbtSource(context.getSource());
  }

  @Override
  public NbtSourceArgument parse(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeString = reader.getCursor();
    final String s = reader.readUnquotedString();
    final NbtSourceArgument nbtSourceArgument = NbtDataRegistry.handleSource(s, registryAccess, new SuggestedParser(reader), false);
    if (nbtSourceArgument == null) {
      final int cursorAfterString = reader.getCursor();
      reader.setCursor(cursorBeforeString);
      throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader), cursorAfterString);
    } else {
      return nbtSourceArgument;
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final StringReader reader = new StringReader(builder.getInput());
    reader.setCursor(builder.getStart());
    final int cursorBeforeString = reader.getCursor();
    final String s = reader.readUnquotedString();
    final int cursorAfterString = reader.getCursor();
    final SuggestedParser suggestedParser = new SuggestedParser(reader);
    final NbtSourceArgument nbtSourceArgument;
    try {
      nbtSourceArgument = NbtDataRegistry.handleSource(s, registryAccess, suggestedParser, true);
      if (nbtSourceArgument == null) {
        reader.setCursor(cursorBeforeString);
        return CommandSource.suggestMatching(NbtDataRegistry.streamSourceTypes(), builder);
      }
    } catch (CommandSyntaxException ignored) {
    }
    return suggestedParser.buildSuggestions(context, builder.createOffset(reader.getCursor()));
  }

  private static final Collection<String> EXAMPLES = List.of("block ~ ~1 ~", "blocks sphere(5) min", "entity @s", "entity Solid", "entity @e[type=pig,limit=1]", "entities @a max", "entities @e random", "store x", "literal {key: value}");

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
