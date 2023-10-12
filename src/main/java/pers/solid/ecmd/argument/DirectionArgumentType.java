package pers.solid.ecmd.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Direction;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class DirectionArgumentType extends EnumArgumentType<DirectionArgument> {
  public DirectionArgumentType() {
    super(DirectionArgument.CODEC, DirectionArgument::values);
  }

  public static DirectionArgumentType create() {
    return new DirectionArgumentType();
  }

  public static Direction getDirection(CommandContext<ServerCommandSource> context, String id) {
    return context.getArgument(id, DirectionArgument.class).apply(context.getSource());
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return ParsingUtil.suggestMatchingEnumWithTooltip(Arrays.asList(DirectionArgument.values()), DirectionArgument::getDisplayName, builder);
  }
}
