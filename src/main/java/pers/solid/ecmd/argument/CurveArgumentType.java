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
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record CurveArgumentType(CommandRegistryAccess registryAccess) implements ArgumentType<CurveArgument<?>> {
  private static final List<String> EXAMPLES = List.of("straight(~~~, ~3~3~3)", "straight(from ^^^ to ^^^5)", "circle(5)");

  public static CurveArgumentType curve(CommandRegistryAccess registryAccess) {
    return new CurveArgumentType(registryAccess);
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
    return CurveArgument.parse(new ParseContext<>(registryAccess, reader, false, false));
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(registryAccess, stringReader, true, false);
    try {
      CurveArgument.parse(parseContext);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parseContext.buildSuggestions(context, builderOffset);
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
