package pers.solid.mod.argument;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.util.math.Direction;

public class DirectionArgumentType extends EnumArgumentType<Direction> {
  public DirectionArgumentType() {
    super(Direction.CODEC, Direction::values);
  }

  public static DirectionArgumentType create() {
    return new DirectionArgumentType();
  }

  public static Direction getDirection(CommandContext<?> context, String id) {
    return context.getArgument(id, Direction.class);
  }
}
