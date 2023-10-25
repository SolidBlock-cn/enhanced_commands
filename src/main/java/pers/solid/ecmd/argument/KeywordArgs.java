package pers.solid.ecmd.argument;

import com.google.common.base.Preconditions;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class KeywordArgs {
  private final KeywordArgsArgumentType type;
  private final Map<String, Object> values;

  public KeywordArgs(KeywordArgsArgumentType type, Map<String, Object> values) {
    this.type = type;
    this.values = values;
  }

  public boolean supportsArg(@NotNull String name) {
    return type.arguments().containsKey(name);
  }

  @SuppressWarnings("unchecked")
  public <T> T getArg(@NotNull String name) {
    Preconditions.checkArgument(type.arguments().containsKey(name), "Invalid arg name: %s", name);
    if (values.containsKey(name)) {
      return (T) values.get(name);
    }
    // The value is not provided
    if (type.defaultValues().containsKey(name)) {
      return (T) type.defaultValues().get(name);
    } else if (type.requiredArguments().contains(name)) {
      throw new IllegalArgumentException("Argument %s is required, but not provided".formatted(name));
    } else {
      return null;
    }
  }

  public int getInt(@NotNull String name) {
    return getArg(name);
  }

  public double getDouble(@NotNull String name) {
    return getArg(name);
  }


  public float getFloat(@NotNull String name) {
    return getArg(name);
  }

  public boolean getBoolean(@NotNull String name) {
    return getArg(name);
  }

  public BlockPos getBlockPos(@NotNull String name, ServerCommandSource source) {
    PosArgument argument = getArg(name);
    return argument.toAbsoluteBlockPos(source);
  }

  public Vec3d getPosition(@NotNull String name, ServerCommandSource source) {
    PosArgument argument = getArg(name);
    return argument.toAbsolutePos(source);
  }
}
