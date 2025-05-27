package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Map;

public record CompoundNbtFunctionArgument(Map<String, @Nullable NbtFunctionArgument> source, boolean allowsMerge) implements NbtFunctionArgument {
  @Override
  public NbtFunction toAbsolute(ServerCommandSource source) throws CommandSyntaxException {
    return new CompoundNbtFunction(IterateUtils.transformFailableImmutableMapValues(this.source, a -> a == null ? null : a.toAbsolute(source)), allowsMerge);
  }
}
