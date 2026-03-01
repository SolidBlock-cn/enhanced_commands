package pers.solid.ecmd.predicate.entity.neoforge;

import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

public class EntitySelectorOptionsExtensionImpl {
  public static void putOption(String id, EntitySelectorOptions.Modifier handler, Predicate<EntitySelectorParser> condition, Component description) {
    // In NeoForge, its access is extended already.
    EntitySelectorOptions.register(id, handler, condition, description);
  }
}
