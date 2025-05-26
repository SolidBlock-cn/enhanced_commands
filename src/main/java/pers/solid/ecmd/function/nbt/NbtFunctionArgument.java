package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.NbtFunctionParser;
import pers.solid.ecmd.util.parse.ParseContext;

public interface NbtFunctionArgument {
  NbtFunction toAbsolute(ServerCommandSource source);

  static <S> NbtFunctionArgument parse(ParseContext<S> parseContext, boolean mustExpectSign, boolean equalsForDefault) throws CommandSyntaxException {
    final NbtFunctionParser<S> n = new NbtFunctionParser<>(parseContext);
    return n.parseFunction(mustExpectSign, equalsForDefault);
  }
}
