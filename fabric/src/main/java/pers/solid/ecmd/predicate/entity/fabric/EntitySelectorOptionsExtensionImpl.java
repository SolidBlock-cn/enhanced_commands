package pers.solid.ecmd.predicate.entity.fabric;

import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.mixins.fabric.EntitySelectorOptionsAccessor;

import java.util.function.Predicate;

public class EntitySelectorOptionsExtensionImpl {
  public static void putOption(String id, EntitySelectorOptions.Modifier handler, Predicate<EntitySelectorParser> condition, Component description) {
    EntitySelectorOptionsAccessor.invokeRegister(id, handler, condition, description);
  }
}
