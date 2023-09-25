package pers.solid.ecmd.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.EnumArgumentType;
import pers.solid.ecmd.util.SuggestionUtil;
import pers.solid.ecmd.util.UnloadedPosBehavior;

import java.util.concurrent.CompletableFuture;

public class UnloadedPosBehaviorArgumentType extends EnumArgumentType<UnloadedPosBehavior> {

  public UnloadedPosBehaviorArgumentType() {
    super(UnloadedPosBehavior.CODEC, UnloadedPosBehavior::values);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return SuggestionUtil.suggestMatchingEnumWithTooltip(UnloadedPosBehavior.VALUES, UnloadedPosBehavior::getDescription, builder);
  }
}
