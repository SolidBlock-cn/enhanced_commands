package pers.solid.ecmd.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.StringRepresentableArgument;
import net.minecraft.core.Direction;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class DirectionArgumentType extends StringRepresentableArgument<DirectionArgument> {
  public DirectionArgumentType() {
    super(DirectionArgument.CODEC, DirectionArgument::values);
  }

  public static DirectionArgumentType direction() {
    return new DirectionArgumentType();
  }

  public static Direction getDirection(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, DirectionArgument.class).apply(context.getSource());
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return ParsingUtil.suggestMatchingEnumWithTooltip(Arrays.asList(DirectionArgument.values()), DirectionArgument::getDisplayName, builder);
  }
}
