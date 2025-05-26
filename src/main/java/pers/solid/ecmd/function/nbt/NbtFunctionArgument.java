package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.NbtFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;

public interface NbtFunctionArgument {
  NbtFunction toAbsolute(ServerCommandSource source);

  static <S> NbtFunctionArgument parse(SuggestedParser<S> parser, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final NbtFunctionSuggestedParser<S> n = new NbtFunctionSuggestedParser<>(parser);
    return n.parseFunction(mustExpectSign, equalsForDefault);
  }
}
