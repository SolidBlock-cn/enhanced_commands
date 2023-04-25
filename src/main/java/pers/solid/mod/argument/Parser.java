package pers.solid.mod.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Parser {
  public final CommandRegistryAccess commandRegistryAccess;

  public final StringReader reader;
  public List<Consumer<SuggestionsBuilder>> suggestions = new ArrayList<>();

  public Parser(CommandRegistryAccess commandRegistryAccess, StringReader reader) {
    this.commandRegistryAccess = commandRegistryAccess;
    this.reader = reader;
  }
}
