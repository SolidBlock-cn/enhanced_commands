package pers.solid.ecmd.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.StringRepresentableArgument;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;

import java.util.concurrent.CompletableFuture;

public class UnloadedPosBehaviorArgumentType extends StringRepresentableArgument<UnloadedPosBehavior> {

  public UnloadedPosBehaviorArgumentType() {
    super(UnloadedPosBehavior.CODEC, UnloadedPosBehavior::values);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return ParsingUtil.suggestMatchingEnumWithTooltip(UnloadedPosBehavior.VALUES, UnloadedPosBehavior::getDescription, builder);
  }
}
