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

public class DirectionArgument extends StringRepresentableArgument<DirectionProvider> {
  public DirectionArgument() {
    super(DirectionProvider.CODEC, DirectionProvider::values);
  }

  public static DirectionArgument direction() {
    return new DirectionArgument();
  }

  public static Direction getDirection(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, DirectionProvider.class).apply(context.getSource());
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return ParsingUtil.suggestMatchingEnumWithTooltip(Arrays.asList(DirectionProvider.values()), DirectionProvider::getDisplayName, builder);
  }
}
