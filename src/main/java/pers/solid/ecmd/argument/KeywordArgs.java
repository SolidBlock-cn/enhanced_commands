package pers.solid.ecmd.argument;

import com.google.common.base.Preconditions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class KeywordArgs {
  private final KeywordArgsArgument type;
  private final Map<String, Object> values;

  public KeywordArgs(KeywordArgsArgument type, Map<String, Object> values) {
    this.type = type;
    this.values = values;
  }

  public boolean supportsArg(@NotNull String name) {
    return type.arguments().containsKey(name);
  }

  @SuppressWarnings("unchecked")
  public <T> T getArg(@NotNull String name) {
    Preconditions.checkArgument(type.arguments().containsKey(name), "Invalid arg propertyName: %s", name);
    if (values.containsKey(name)) {
      return (T) values.get(name);
    }
    // The probability is not provided
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

  public BlockPos getBlockPos(@NotNull String name, CommandSourceStack source) {
    Coordinates argument = getArg(name);
    return argument.getBlockPos(source);
  }

  public Vec3 getPosition(@NotNull String name, CommandSourceStack source) {
    Coordinates argument = getArg(name);
    return argument.getPosition(source);
  }
}
