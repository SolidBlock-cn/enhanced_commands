package pers.solid.ecmd.argument;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Function;

public enum DirectionProvider implements StringRepresentable, Function<@NotNull PositionProvider, @NotNull Direction> {
  DOWN(Direction.DOWN),
  UP(Direction.UP),
  NORTH(Direction.NORTH),
  SOUTH(Direction.SOUTH),
  WEST(Direction.WEST),
  EAST(Direction.EAST),
  FRONT("front", positionProvider -> {
    final Vec2 rotation = positionProvider.getRotation$ec();
    if (rotation.x > 60) {
      return Direction.DOWN;
    } else if (rotation.x < -60) {
      return Direction.UP;
    } else {
      return Direction.fromYRot(rotation.y);
    }
  }),
  BACK("back", FRONT.function.andThen(Direction::getOpposite)),
  FRONT_HORIZONTAL("front_horizontal", positionProvider -> Direction.fromYRot(positionProvider.getRotation$ec().y)),
  BACK_HORIZONTAL("back_horizontal", FRONT_HORIZONTAL.function.andThen(Direction::getOpposite)),
  FRONT_VERTICAL("front_vertical", positionProvider -> positionProvider.getRotation$ec().x > 0 ? Direction.UP : Direction.DOWN),
  BACK_VERTICAL("back_vertical", positionProvider -> positionProvider.getRotation$ec().x > 0 ? Direction.DOWN : Direction.UP),
  LEFT("left", FRONT_HORIZONTAL.function.andThen(Direction::getCounterClockWise)),
  RIGHT("right", FRONT_HORIZONTAL.function.andThen(Direction::getClockWise)),
  RANDOM("random", positionProvider -> Direction.getRandom(positionProvider.getWorld$ec().getRandom())),
  RANDOM_HORIZONTAL("random_horizontal", positionProvider -> Direction.Plane.HORIZONTAL.getRandomDirection(positionProvider.getWorld$ec().getRandom())),
  RANDOM_VERTICAL("random_vertical", positionProvider -> Direction.Plane.VERTICAL.getRandomDirection(positionProvider.getWorld$ec().getRandom()));

  public static final StringIdentifiableCodec<DirectionProvider> CODEC = StringIdentifiableCodec.create(DirectionProvider.values());
  private final String name;
  private final Function<PositionProvider, Direction> function;

  DirectionProvider(@NotNull Direction direction) {
    this.name = direction.getSerializedName();
    this.function = positionProvider -> direction;
  }

  DirectionProvider(String name, Function<PositionProvider, Direction> function) {
    this.name = name;
    this.function = function;
  }

  @Override
  public @NotNull Direction apply(@NotNull PositionProvider positionProvider) {
    return function.apply(positionProvider);
  }

  public @NotNull Direction apply(@NotNull CommandSourceStack source) {
    return function.apply(source);
  }

  @Override
  public @NotNull String getSerializedName() {
    return name;
  }

  public MutableComponent getDisplayName() {
    return Component.translatable("enhanced_commands.direction." + name);
  }
}
