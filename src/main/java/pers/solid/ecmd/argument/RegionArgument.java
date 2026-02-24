package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import pers.solid.ecmd.mixins.accessor.CommandContextAccessor;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionProvider;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record RegionArgument(CommandBuildContext commandBuildContext) implements ArgumentType<RegionProvider<?>> {
  private static final List<String> EXAMPLES = List.of("cuboid(1 1 1, 2 2 2)", "sphere(3)", "cyl(3, 2)", "outline(cuboid(~~~, ~~~5))");

  public static RegionArgument region(CommandBuildContext commandBuildContext) {
    return new RegionArgument(commandBuildContext);
  }

  public static RegionProvider<?> getRegionArgument(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, RegionProvider.class);
  }

  /**
   * @see net.minecraft.commands.arguments.coordinates.Vec3Argument#getVec3(CommandContext, String)
   */
  public static Region getRegion(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
    try {
      if (!((CommandContextAccessor<?>) context).getArguments().containsKey(name)) {
        final RegionProvider<?> sourceArg = context.getSource().getExtraArgument$ec("region", RegionProvider.class);
        if (sourceArg != null) {
          return sourceArg.toAbsoluteRegion(context.getSource());
        }
      }
      return context.getArgument(name, RegionProvider.class).toAbsoluteRegion(context.getSource());
    } catch (RuntimeException e) {
      if (e.getCause() instanceof CommandSyntaxException commandSyntaxException) {
        throw commandSyntaxException;
      } else {
        throw e;
      }
    }
  }

  @Override
  public RegionProvider<?> parse(StringReader reader) throws CommandSyntaxException {
    return RegionProvider.parse(new ParseContext<>(commandBuildContext, reader, false, false));
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, stringReader, true, false);
    try {
      RegionProvider.parse(parseContext);
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
