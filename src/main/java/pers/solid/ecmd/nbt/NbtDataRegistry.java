package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.ecmd.argument.SuggestedParser;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class NbtDataRegistry {
  private static final Map<String, Handler<? extends NbtSourceArgument>> SOURCES = new HashMap<>();
  private static final Map<String, Handler<? extends NbtTargetArgument>> TARGETS = new HashMap<>();

  public static void registerSource(String type, Handler<? extends NbtSourceArgument> handler) {
    SOURCES.put(type, handler);
  }

  public static void registerTarget(String type, Handler<? extends NbtTargetArgument> handler) {
    TARGETS.put(type, handler);
  }

  public static <T extends NbtSourceArgument & NbtTargetArgument> void register(String type, Handler<T> handler) {
    registerSource(type, handler);
    registerTarget(type, handler);
  }

  public static NbtSourceArgument handleSource(String type, CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    if (!SOURCES.containsKey(type)) {
      return null;
    }
    return SOURCES.get(type).handle(registryAccess, parser, suggestionsOnly);
  }

  public static NbtTargetArgument handleTarget(String type, CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    if (!TARGETS.containsKey(type)) {
      return null;
    }
    return TARGETS.get(type).handle(registryAccess, parser, suggestionsOnly);
  }

  public static Stream<String> streamSourceTypes() {
    return SOURCES.keySet().stream();
  }

  public static Stream<String> streamTargetTypes() {
    return TARGETS.keySet().stream();
  }

  private NbtDataRegistry() {
  }

  public interface Handler<T> {
    T handle(CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException;
  }

  public static void init() {
    register("block", BlockNbtDataArgument::handle);
    registerSource("blocks", (registryAccess, parser, suggestionsOnly) -> BlocksNbtDataArgument.handle(registryAccess, parser, suggestionsOnly, true));
    registerTarget("blocks", (registryAccess, parser, suggestionsOnly) -> BlocksNbtDataArgument.handle(registryAccess, parser, suggestionsOnly, false));
    register("entity", EntityNbtDataArgument::handle);
    registerSource("entities", (registryAccess, suggestedParser, suggestionsOnly) -> EntitiesNbtDataArgument.handle(suggestedParser, true));
    registerTarget("entities", (registryAccess, suggestedParser, suggestionsOnly) -> EntitiesNbtDataArgument.handle(suggestedParser, false));
    registerSource("literal", LiteralNbtSource::handle);
    register("storage", StorageNbtDataArgument::handle);
  }
}
