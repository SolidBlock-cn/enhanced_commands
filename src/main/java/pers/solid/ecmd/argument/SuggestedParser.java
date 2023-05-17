package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class SuggestedParser {
  public final CommandRegistryAccess commandRegistryAccess;

  public final StringReader reader;
  public List<BiConsumer<SuggestionsBuilder, CommandContext<?>>> suggestions = new ArrayList<>();

  public SuggestedParser(CommandRegistryAccess commandRegistryAccess, StringReader reader) {
    this.commandRegistryAccess = commandRegistryAccess;
    this.reader = reader;
  }
}
