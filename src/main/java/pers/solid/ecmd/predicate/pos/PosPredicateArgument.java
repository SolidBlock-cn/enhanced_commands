package pers.solid.ecmd.predicate.pos;

import net.minecraft.commands.CommandSourceStack;

@FunctionalInterface
public interface PosPredicateArgument {
  PosPredicate toAbsolutePosPredicate(CommandSourceStack source);
}
