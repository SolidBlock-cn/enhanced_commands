package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtFunctionParser;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.function.nbt.NbtFunctionArgument;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record LiteralNbtDataArgument(NbtFunctionArgument nbtFunctionArgument) implements NbtSourceArgument<MutableObject<NbtCompound>>, NbtTargetArgument<MutableObject<NbtCompound>> {

  public static LiteralNbtData handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parseContext.reader());
    final NbtFunction nbtFunction = new NbtFunctionParser<>(parseContext).parseFunction(false, false);
    return new LiteralNbtData(nbtFunction);
  }

  private @NotNull LiteralNbtData getLiteralNbtData(ServerCommandSource source) throws CommandSyntaxException {
    return new LiteralNbtData(nbtFunctionArgument.toAbsolute(source));
  }

  @Override
  public NbtSource<MutableObject<NbtCompound>> getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getLiteralNbtData(source);
  }

  @Override
  public NbtTarget<MutableObject<NbtCompound>> getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getLiteralNbtData(source);
  }
}
