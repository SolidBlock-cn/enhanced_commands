package pers.solid.ecmd.function.nbt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Map;

public record ListOpsNbtFunctionArgument(List<NbtFunctionArgument> valueReplacements, Map<Integer, NbtFunctionArgument> positionalFunctions, Map<Integer, List<NbtFunctionArgument>> positionalInsertions) implements NbtFunctionArgument {
  @Override
  public NbtFunction toAbsolute(ServerCommandSource source) {
    return new ListOpsNbtFunction(ImmutableList.copyOf(Lists.transform(valueReplacements, a -> a.toAbsolute(source))), ImmutableMap.copyOf(Maps.transformValues(positionalFunctions, input -> input.toAbsolute(source))), ImmutableMap.copyOf(Maps.transformValues(positionalInsertions, list -> ImmutableList.copyOf(Lists.transform(list, a -> a.toAbsolute(source))))));
  }
}
