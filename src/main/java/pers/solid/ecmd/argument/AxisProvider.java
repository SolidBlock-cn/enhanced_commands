package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableList;
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

public enum AxisProvider implements StringRepresentable, Function<@NotNull PositionProvider, Direction.@NotNull Axis> {
  X(Direction.Axis.X),
  Y(Direction.Axis.Y),
  Z(Direction.Axis.Z),
  FRONT_BACK("front_back", positionProvider -> {
    final Vec2 rotation = positionProvider.getRotation$ec();
    if (rotation.x > 60 || rotation.x < -60) {
      return Direction.Axis.Y;
    } else {
      return Direction.fromYRot(rotation.y).getAxis();
    }
  }),
  FRONT_BACK_HORIZONTAL("front_back_horizontal", positionProvider -> Direction.fromYRot(positionProvider.getRotation$ec().y).getAxis()),
  LEFT_RIGHT("left_right", positionProvider -> Direction.fromYRot(positionProvider.getRotation$ec().y).getClockWise().getAxis()),
  RANDOM("random", positionProvider -> Direction.Axis.getRandom(positionProvider.getWorld$ec().getRandom())),
  RANDOM_HORIZONTAL("random_horizontal", positionProvider -> positionProvider.getWorld$ec().getRandom().nextBoolean() ? Direction.Axis.X : Direction.Axis.Z);

  public static final ImmutableList<AxisProvider> VALUES = ImmutableList.copyOf(values());
  public static final ImmutableList<AxisProvider> VALUES_EXCEPT_RANDOM = VALUES.subList(0, VALUES.size() - 2);
  public static final StringIdentifiableCodec<AxisProvider> CODEC = StringIdentifiableCodec.create(AxisProvider.values());
  public static final StringIdentifiableCodec<AxisProvider> CODEC_EXCLUDING_RANDOM = StringIdentifiableCodec.create(VALUES_EXCEPT_RANDOM.toArray(AxisProvider[]::new));
  private final String name;
  private final Function<PositionProvider, Direction.Axis> function;

  AxisProvider(Direction.Axis axis) {
    this.name = axis.getSerializedName();
    this.function = positionProvider -> axis;
  }

  AxisProvider(String name, Function<PositionProvider, Direction.Axis> function) {
    this.name = name;
    this.function = function;
  }

  @Override
  public @NotNull Direction.Axis apply(@NotNull PositionProvider positionProvider) {
    return function.apply(positionProvider);
  }

  public @NotNull Direction.Axis apply(@NotNull CommandSourceStack source) {
    return function.apply(source);
  }

  @Override
  public @NotNull String getSerializedName() {
    return name;
  }

  public MutableComponent getDisplayName() {
    return Component.translatable("enhanced_commands.axis." + name);
  }
}
