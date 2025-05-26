package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class NbtDataRegistry {
  private static final Map<String, Parser<? extends NbtSourceArgument<?>>> SOURCES = new HashMap<>();
  private static final Map<String, Parser<? extends NbtTargetArgument<?>>> TARGETS = new HashMap<>();

  public static void registerSource(String type, Parser<? extends NbtSourceArgument<?>> handler) {
    SOURCES.put(type, handler);
  }

  public static void registerTarget(String type, Parser<? extends NbtTargetArgument<?>> handler) {
    TARGETS.put(type, handler);
  }

  public static <T extends NbtSourceArgument<?> & NbtTargetArgument<?>> void register(String type, Parser<T> handler) {
    registerSource(type, handler);
    registerTarget(type, handler);
  }

  public static NbtSourceArgument<?> handleSource(String type, ParseContext<?> parseContext) throws CommandSyntaxException {
    if (!SOURCES.containsKey(type)) {
      return null;
    }
    return SOURCES.get(type).parse(parseContext);
  }

  public static NbtTargetArgument<?> handleTarget(String type, ParseContext<?> parseContext) throws CommandSyntaxException {
    if (!TARGETS.containsKey(type)) {
      return null;
    }
    return TARGETS.get(type).parse(parseContext);
  }

  public static Stream<String> streamSourceTypes() {
    return SOURCES.keySet().stream();
  }

  public static Stream<String> streamTargetTypes() {
    return TARGETS.keySet().stream();
  }

  private NbtDataRegistry() {
  }

  public static void init() {
    register("block", BlocksNbtDataArgument::handle);
    register("entity", EntitiesNbtDataArgument::handle);
    register("literal", LiteralNbtData::handle);
    register("storage", StorageNbtDataArgument::handle);
  }
}
