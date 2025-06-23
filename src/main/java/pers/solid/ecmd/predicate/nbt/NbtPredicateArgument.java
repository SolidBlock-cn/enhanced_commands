package pers.solid.ecmd.predicate.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.util.parse.ParseContext;

public interface NbtPredicateArgument {
  NbtPredicate toAbsolute(ServerCommandSource source);

  static <S> NbtPredicate parse(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final NbtPredicateParser<S> n = new NbtPredicateParser<>(parseContext);
    return n.parsePredicate(mustExpectSign, equalsForDefault);
  }
}
