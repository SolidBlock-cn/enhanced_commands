package pers.solid.ecmd.function.nbt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record CompoundNbtFunctionArgument(Map<String, @Nullable NbtFunctionArgument> source, boolean allowsMerge) implements NbtFunctionArgument {
  @Override
  public NbtFunction toAbsolute(ServerCommandSource source) {
    return new CompoundNbtFunction(ImmutableMap.copyOf(Maps.transformValues(this.source, a -> a == null ? null : a.toAbsolute(source))), allowsMerge);
  }
}
