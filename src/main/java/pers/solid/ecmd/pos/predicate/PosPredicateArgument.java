package pers.solid.ecmd.pos.predicate;

import net.minecraft.commands.CommandSourceStack;

@FunctionalInterface
public interface PosPredicateArgument {
  PosPredicate toAbsolutePosPredicate(CommandSourceStack source);
}
