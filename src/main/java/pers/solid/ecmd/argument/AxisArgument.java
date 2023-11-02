package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public enum AxisArgument implements StringIdentifiable, Function<@NotNull ServerCommandSource, Direction.@NotNull Axis> {
  X(Direction.Axis.X),
  Y(Direction.Axis.Y),
  Z(Direction.Axis.Z),
  FRONT_BACK("front_back", source -> {
    final Vec2f rotation = source.getRotation();
    if (rotation.x > 60 || rotation.x < -60) {
      return Direction.Axis.Y;
    } else {
      return Direction.fromRotation(rotation.y).getAxis();
    }
  }),
  FRONT_BACK_HORIZONTAL("front_back_horizontal", source -> Direction.fromRotation(source.getRotation().y).getAxis()),
  LEFT_RIGHT("left_right", source -> Direction.fromRotation(source.getRotation().y).rotateYClockwise().getAxis()),
  RANDOM("random", source -> Direction.Axis.pickRandomAxis(source.getWorld().getRandom())),
  RANDOM_HORIZONTAL("random_horizontal", source -> source.getWorld().getRandom().nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);

  public static final ImmutableList<AxisArgument> VALUES = ImmutableList.copyOf(values());
  public static final ImmutableList<AxisArgument> VALUES_EXCEPT_RANDOM = VALUES.subList(0, VALUES.size() - 2);
  public static final Codec<AxisArgument> CODEC = StringIdentifiable.createCodec(AxisArgument::values);
  public static final Codec<AxisArgument> CODEC_EXCLUDING_RANDOM = StringIdentifiable.createCodec(() -> VALUES_EXCEPT_RANDOM.toArray(AxisArgument[]::new));
  private final String name;
  private final Function<ServerCommandSource, Direction.Axis> function;

  AxisArgument(Direction.Axis axis) {
    this.name = axis.asString();
    this.function = source -> axis;
  }

  AxisArgument(String name, Function<ServerCommandSource, Direction.Axis> function) {
    this.name = name;
    this.function = function;
  }

  @Override
  public @NotNull Direction.Axis apply(@NotNull ServerCommandSource source) {
    return function.apply(source);
  }

  @Override
  public String asString() {
    return name;
  }

  public MutableText getDisplayName() {
    return Text.translatable("enhanced_commands.axis." + name);
  }
}
