package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.ecmd.util.SuggestionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SuggestedParser {
  public final CommandRegistryAccess commandRegistryAccess;

  public final StringReader reader;
  public List<SuggestionProvider> suggestions = new ArrayList<>();

  public SuggestedParser(CommandRegistryAccess commandRegistryAccess, StringReader reader) {
    this.commandRegistryAccess = commandRegistryAccess;
    this.reader = reader;
  }

  public CompletableFuture<Suggestions> buildSuggestions(CommandContext<?> context, SuggestionsBuilder builder) {
    for (SuggestionProvider suggestion : suggestions) {
      if (suggestion instanceof SuggestionProvider.Modifying modifying) {
        return modifying.apply(context, builder);
      } else {
        suggestion.accept(context, builder);
      }
    }
    return builder.buildFuture();
  }
}
