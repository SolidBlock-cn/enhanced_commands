package pers.solid.ecmd.argument;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Function;

public enum DirectionArgument implements StringIdentifiable, Function<@NotNull PositionProvider, @NotNull Direction> {
  DOWN(Direction.DOWN),
  UP(Direction.UP),
  NORTH(Direction.NORTH),
  SOUTH(Direction.SOUTH),
  WEST(Direction.WEST),
  EAST(Direction.EAST),
  FRONT("front", positionProvider -> {
    final Vec2f rotation = positionProvider.getRotation$ec();
    if (rotation.x > 60) {
      return Direction.DOWN;
    } else if (rotation.x < -60) {
      return Direction.UP;
    } else {
      return Direction.fromHorizontalDegrees(rotation.y);
    }
  }),
  BACK("back", FRONT.function.andThen(Direction::getOpposite)),
  FRONT_HORIZONTAL("front_horizontal", positionProvider -> Direction.fromHorizontalDegrees(positionProvider.getRotation$ec().y)),
  BACK_HORIZONTAL("back_horizontal", FRONT_HORIZONTAL.function.andThen(Direction::getOpposite)),
  FRONT_VERTICAL("front_vertical", positionProvider -> positionProvider.getRotation$ec().x > 0 ? Direction.UP : Direction.DOWN),
  BACK_VERTICAL("back_vertical", positionProvider -> positionProvider.getRotation$ec().x > 0 ? Direction.DOWN : Direction.UP),
  LEFT("left", FRONT_HORIZONTAL.function.andThen(Direction::rotateYCounterclockwise)),
  RIGHT("right", FRONT_HORIZONTAL.function.andThen(Direction::rotateYClockwise)),
  RANDOM("random", positionProvider -> Direction.random(positionProvider.getWorld$ec().getRandom())),
  RANDOM_HORIZONTAL("random_horizontal", positionProvider -> Direction.Type.HORIZONTAL.random(positionProvider.getWorld$ec().getRandom())),
  RANDOM_VERTICAL("random_vertical", positionProvider -> Direction.Type.VERTICAL.random(positionProvider.getWorld$ec().getRandom()));

  public static final StringIdentifiableCodec<DirectionArgument> CODEC = StringIdentifiableCodec.create(DirectionArgument.values());
  private final String name;
  private final Function<PositionProvider, Direction> function;

  DirectionArgument(@NotNull Direction direction) {
    this.name = direction.asString();
    this.function = positionProvider -> direction;
  }

  DirectionArgument(String name, Function<PositionProvider, Direction> function) {
    this.name = name;
    this.function = function;
  }

  @Override
  public @NotNull Direction apply(@NotNull PositionProvider positionProvider) {
    return function.apply(positionProvider);
  }

  public @NotNull Direction apply(@NotNull ServerCommandSource source) {
    return function.apply(source);
  }

  @Override
  public String asString() {
    return name;
  }

  public MutableText getDisplayName() {
    return Text.translatable("enhanced_commands.direction." + name);
  }
}
