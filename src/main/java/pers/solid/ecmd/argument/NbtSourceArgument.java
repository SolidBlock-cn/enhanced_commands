package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import pers.solid.ecmd.nbt.NbtDataRegistry;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record NbtSourceArgument(CommandBuildContext commandBuildContext) implements ArgumentType<NbtSource<?>> {
  public static NbtSourceArgument nbtSource(CommandBuildContext commandBuildContext) {
    return new NbtSourceArgument(commandBuildContext);
  }

  public static NbtSource<?> getNbtSource(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
    return ((NbtSource<?>) context.getArgument(name, NbtSource.class));
  }

  @Override
  public NbtSource<?> parse(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeString = reader.getCursor();
    final String s = reader.readUnquotedString();
    final NbtSource<?> nbtSource = NbtDataRegistry.handleSource(s, new ParseContext<>(commandBuildContext, reader, false, true));
    if (nbtSource == null) {
      final int cursorAfterString = reader.getCursor();
      reader.setCursor(cursorBeforeString);
      throw EnhancedCommandSyntaxException.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader), cursorAfterString);
    } else {
      return nbtSource;
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final StringReader reader = new StringReader(builder.getInput());
    reader.setCursor(builder.getStart());
    final int cursorBeforeString = reader.getCursor();
    final String s = reader.readUnquotedString();
    final int cursorAfterString = reader.getCursor();
    final NbtSource<?> nbtSource;
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, reader, true, true);
    try {
      nbtSource = NbtDataRegistry.handleSource(s, parseContext);
      if (nbtSource == null) {
        reader.setCursor(cursorBeforeString);
        return SharedSuggestionProvider.suggest(NbtDataRegistry.streamSourceTypes(), builder);
      }
    } catch (CommandSyntaxException ignored) {
    }
    return parseContext.buildSuggestions(context, builder.createOffset(reader.getCursor()));
  }

  private static final Collection<String> EXAMPLES = List.of("block ~ ~1 ~", "blocks sphere(5) min", "entity @s", "entity Solid", "entity @e[type=pig,limit=1]", "entities @a max", "entities @e random", "store x", "literal {key: probability}");

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
