package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;
import java.util.Map;

public record ListOpsNbtFunctionArgument(List<NbtFunctionArgument> valueReplacements, Map<Integer, NbtFunctionArgument> positionalFunctions, Map<Integer, List<NbtFunctionArgument>> positionalInsertions) implements NbtFunctionArgument {
  @Override
  public NbtFunction toAbsolute(ServerCommandSource source) throws CommandSyntaxException {
    return new ListOpsNbtFunction(IterateUtils.transformFailableImmutableList(valueReplacements, a -> a.toAbsolute(source)), IterateUtils.transformFailableImmutableMapValues(positionalFunctions, input -> input.toAbsolute(source)), IterateUtils.transformFailableImmutableMapValues(positionalInsertions, list -> IterateUtils.transformFailableImmutableList(list, a -> a.toAbsolute(source))));
  }
}
