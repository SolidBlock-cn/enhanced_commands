package pers.solid.ecmd.predicate.pos;

import net.minecraft.server.command.ServerCommandSource;

@FunctionalInterface
public interface PosPredicateArgument {
  PosPredicate toAbsolutePosPredicate(ServerCommandSource source);
}
