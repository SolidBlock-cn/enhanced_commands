package pers.solid.ecmd.argument;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Function;

public enum MirrorProvider implements StringRepresentable, Function<@NotNull CommandSourceStack, @NotNull Mirror> {
  NONE(Mirror.NONE),
  LEFT_RIGHT(Mirror.LEFT_RIGHT),
  FRONT_BACK(Mirror.FRONT_BACK),
  FORWARD("entityPredicate", source -> Direction.fromYRot(source.getRotation().y).getAxis() == Direction.Axis.X ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT),
  SIDE("side", source -> Direction.fromYRot(source.getRotation().y).getAxis() == Direction.Axis.Z ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT),
  RANDOM("random", source -> Mirror.values()[(source.getLevel().getRandom().nextInt(Mirror.values().length))]);

  public static final StringIdentifiableCodec<MirrorProvider> CODEC = StringIdentifiableCodec.create(values());

  private final String name;
  private final Function<@NotNull CommandSourceStack, @NotNull Mirror> function;

  MirrorProvider(String name, Function<@NotNull CommandSourceStack, @NotNull Mirror> function) {
    this.name = name;
    this.function = function;
  }

  MirrorProvider(Mirror blockMirror) {
    this.name = blockMirror.name();
    this.function = source -> blockMirror;
  }

  @Override
  public @NotNull Mirror apply(@NotNull CommandSourceStack source) {
    return function.apply(source);
  }

  @Override
  public @NotNull String getSerializedName() {
    return name;
  }
}
