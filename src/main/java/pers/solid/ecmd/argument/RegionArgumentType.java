package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record RegionArgumentType(CommandRegistryAccess commandRegistryAccess) implements ArgumentType<RegionArgument<?>> {
  public static RegionArgumentType region(CommandRegistryAccess commandRegistryAccess) {
    return new RegionArgumentType(commandRegistryAccess);
  }

  /**
   * @see net.minecraft.command.argument.Vec3ArgumentType#getVec3(CommandContext, String)
   */
  public static Region getRegion(CommandContext<ServerCommandSource> context, String name) {
    return context.getArgument(name, RegionArgument.class).toAbsoluteRegion(context.getSource());
  }

  @Override
  public RegionArgument<?> parse(StringReader reader) throws CommandSyntaxException {
    return RegionArgument.parse(new SuggestedParser(commandRegistryAccess, reader), false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final SuggestedParser parser = new SuggestedParser(commandRegistryAccess, stringReader);
    try {
      RegionArgument.parse(parser, true);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parser.buildSuggestions(context, builderOffset);
  }

  @Override
  public Collection<String> getExamples() {
    return List.of("cuboid(1 1 1)");
  }
}
