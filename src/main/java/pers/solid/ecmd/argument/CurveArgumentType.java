package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.curve.Curve;
import pers.solid.ecmd.curve.CurveArgument;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record CurveArgumentType(CommandRegistryAccess commandRegistryAccess) implements ArgumentType<CurveArgument<?>> {
  private static final List<String> EXAMPLES = List.of("straight(~~~, ~3~3~3)", "straight(from ^^^ to ^^^5)", "circle(5)");

  public static CurveArgumentType curve(CommandRegistryAccess commandRegistryAccess) {
    return new CurveArgumentType(commandRegistryAccess);
  }

  public static Curve getCurve(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
    try {
      return context.getArgument(name, CurveArgument.class).toAbsoluteRegion(context.getSource());
    } catch (RuntimeException e) {
      if (e.getCause() instanceof CommandSyntaxException commandSyntaxException) {
        throw commandSyntaxException;
      } else {
        throw e;
      }
    }
  }

  @Override
  public CurveArgument<?> parse(StringReader reader) throws CommandSyntaxException {
    return CurveArgument.parse(commandRegistryAccess, new SuggestedParser(reader), false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final SuggestedParser parser = new SuggestedParser(stringReader);
    try {
      CurveArgument.parse(commandRegistryAccess, parser, true);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parser.buildSuggestions(context, builderOffset);
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
