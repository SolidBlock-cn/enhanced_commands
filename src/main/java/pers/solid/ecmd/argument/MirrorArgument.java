package pers.solid.ecmd.argument;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Function;

public enum MirrorArgument implements StringIdentifiable, Function<@NotNull ServerCommandSource, @NotNull BlockMirror> {
  NONE(BlockMirror.NONE),
  LEFT_RIGHT(BlockMirror.LEFT_RIGHT),
  FRONT_BACK(BlockMirror.FRONT_BACK),
  FORWARD("entityPredicate", source -> Direction.fromRotation(source.getRotation().y).getAxis() == Direction.Axis.X ? BlockMirror.FRONT_BACK : BlockMirror.LEFT_RIGHT),
  SIDE("side", source -> Direction.fromRotation(source.getRotation().y).getAxis() == Direction.Axis.Z ? BlockMirror.FRONT_BACK : BlockMirror.LEFT_RIGHT),
  RANDOM("random", source -> BlockMirror.values()[(source.getWorld().getRandom().nextInt(BlockMirror.values().length))]);

  public static final StringIdentifiableCodec<MirrorArgument> CODEC = StringIdentifiableCodec.create(values());

  private final String name;
  private final Function<@NotNull ServerCommandSource, @NotNull BlockMirror> function;

  MirrorArgument(String name, Function<@NotNull ServerCommandSource, @NotNull BlockMirror> function) {
    this.name = name;
    this.function = function;
  }

  MirrorArgument(BlockMirror blockMirror) {
    this.name = blockMirror.name();
    this.function = source -> blockMirror;
  }

  @Override
  public @NotNull BlockMirror apply(@NotNull ServerCommandSource source) {
    return function.apply(source);
  }

  @Override
  public String asString() {
    return name;
  }
}
