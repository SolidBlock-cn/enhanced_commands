package pers.solid.ecmd.argument;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public enum DirectionArgument implements StringIdentifiable, Function<@NotNull ServerCommandSource, @NotNull Direction> {
  DOWN(Direction.DOWN),
  UP(Direction.UP),
  NORTH(Direction.NORTH),
  SOUTH(Direction.SOUTH),
  WEST(Direction.WEST),
  EAST(Direction.EAST),
  FRONT("front", source -> {
    final Vec2f rotation = source.getRotation();
    if (rotation.x > 60) {
      return Direction.UP;
    } else if (rotation.x < -60) {
      return Direction.DOWN;
    } else {
      return Direction.fromRotation(rotation.y);
    }
  }),
  BACK("back", FRONT.function.andThen(Direction::getOpposite)),
  FRONT_HORIZONTAL("front_horizontal", source -> Direction.fromRotation(source.getRotation().y)),
  BACK_HORIZONTAL("back_horizontal", FRONT_HORIZONTAL.function.andThen(Direction::getOpposite)),
  FRONT_VERTICAL("front_vertical", source -> source.getRotation().x > 0 ? Direction.UP : Direction.DOWN),
  BACK_VERTICAL("from_vertical", source -> source.getRotation().x > 0 ? Direction.DOWN : Direction.UP),
  LEFT("left", FRONT_HORIZONTAL.function.andThen(Direction::rotateYCounterclockwise)),
  RIGHT("right", FRONT_HORIZONTAL.function.andThen(Direction::rotateYClockwise)),
  RANDOM("random", source -> Direction.random(source.getWorld().getRandom())),
  RANDOM_HORIZONTAL("random_horizontal", source -> Direction.Type.HORIZONTAL.random(source.getWorld().getRandom())),
  RANDOM_VERTICAL("random_vertical", source -> Direction.Type.VERTICAL.random(source.getWorld().getRandom()));

  public static final Codec<DirectionArgument> CODEC = StringIdentifiable.createCodec(DirectionArgument::values);
  private final String name;
  private final Function<ServerCommandSource, Direction> function;

  DirectionArgument(@NotNull Direction direction) {
    this.name = direction.asString();
    this.function = source -> direction;
  }

  DirectionArgument(String name, Function<ServerCommandSource, Direction> function) {
    this.name = name;
    this.function = function;
  }

  @Override
  public @NotNull Direction apply(@NotNull ServerCommandSource serverCommandSource) {
    return function.apply(serverCommandSource);
  }

  @Override
  public String asString() {
    return name;
  }

  public MutableText getDisplayName() {
    return Text.translatable("enhancedCommands.direction." + name);
  }
}
