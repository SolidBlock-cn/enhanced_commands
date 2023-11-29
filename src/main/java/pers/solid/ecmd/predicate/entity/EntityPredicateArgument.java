package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.lang3.function.FailableFunction;

public interface EntityPredicateArgument extends FailableFunction<ServerCommandSource, EntityPredicate, CommandSyntaxException> {
  static EntityPredicateArgument of(EntitySelector entitySelector) {
    return source -> new SelectorEntityPredicate(entitySelector, source);
  }

  @Override
  EntityPredicate apply(ServerCommandSource source) throws CommandSyntaxException;
}
