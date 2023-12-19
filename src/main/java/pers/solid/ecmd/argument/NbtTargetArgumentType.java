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
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.nbt.NbtTargetArgument;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record NbtTargetArgumentType(CommandRegistryAccess registryAccess) implements ArgumentType<NbtTargetArgument> {
  public static NbtTargetArgumentType nbtTarget(CommandRegistryAccess registryAccess) {
    return new NbtTargetArgumentType(registryAccess);
  }

  public static NbtTargetArgument getNbtTargetArgument(CommandContext<?> context, String name) {
    return context.getArgument(name, NbtTargetArgument.class);
  }

  public static NbtTarget getNbtTarget(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
    return getNbtTargetArgument(context, name).getNbtTarget(context.getSource());
  }

  @Override
  public NbtTargetArgument parse(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeString = reader.getCursor();
    final String s = reader.readUnquotedString();
    final NbtTargetArgument nbtTargetArgument = NbtDataRegistry.handleTarget(s, registryAccess, new SuggestedParser(reader), false);
    if (nbtTargetArgument == null) {
      final int cursorAfterString = reader.getCursor();
      reader.setCursor(cursorBeforeString);
      throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader), cursorAfterString);
    } else {
      return nbtTargetArgument;
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final StringReader reader = new StringReader(builder.getInput());
    reader.setCursor(builder.getStart());
    final int cursorBeforeString = reader.getCursor();
    final String s = reader.readUnquotedString();
    final SuggestedParser suggestedParser = new SuggestedParser(reader);
    final NbtTargetArgument nbtTargetArgument;
    try {
      nbtTargetArgument = NbtDataRegistry.handleTarget(s, registryAccess, suggestedParser, true);
      if (nbtTargetArgument == null) {
        reader.setCursor(cursorBeforeString);
        return CommandSource.suggestMatching(NbtDataRegistry.streamTargetTypes(), builder);
      }
    } catch (CommandSyntaxException ignored) {
    }
    return suggestedParser.buildSuggestions(context, builder.createOffset(reader.getCursor()));
  }

  private static final Collection<String> EXAMPLES = List.of("block ~ ~1 ~", "blocks sphere(5)", "entity @s", "entity Solid", "entity @e[type=pig,limit=1]", "entities @a", "entities @e", "store x");

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
