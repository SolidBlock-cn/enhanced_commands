package pers.solid.ecmd.predicate.entity;

import net.minecraft.command.EntitySelector;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Function;

public interface EntityPredicateArgument extends Function<ServerCommandSource, EntityPredicate> {
  static EntityPredicateArgument of(EntitySelector entitySelector) {
    return source -> new SelectorEntityPredicate(entitySelector, source);
  }
}
