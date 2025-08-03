package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * 此类对应于 {@link EntitySelector#positionOffset}，但由于该字段为 {@code Function}，无法直接进行序列化和反序列化，因此需要在解析实体选择器时，也就是在 {@link EntitySelectorReader#build()} 中，利用 mixin 将其 x、y、z 存储在字段中。
 *
 * @param x 对应实体选择器的 x 参数。
 * @param y 对应实体选择器的 y 参数。
 * @param z 对应实体选择器的 z 参数。
 */
public record PositionOffsetInfo(@Nullable Double x, @Nullable Double y, @Nullable Double z) implements Function<Vec3d, Vec3d> {
  public static final PositionOffsetInfo NO_OP = new PositionOffsetInfo(null, null, null);
  public static final MapCodec<PositionOffsetInfo> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.DOUBLE.optionalFieldOf("x").forGetter(o -> Optional.ofNullable(o.x())),
      Codec.DOUBLE.optionalFieldOf("y").forGetter(o -> Optional.ofNullable(o.y())),
      Codec.DOUBLE.optionalFieldOf("z").forGetter(o -> Optional.ofNullable(o.z()))
  ).apply(i, (x, y, z) -> of(x.orElse(null), y.orElse(null), z.orElse(null))));

  public static PositionOffsetInfo of(@Nullable Double x, @Nullable Double y, @Nullable Double z) {
    if (x == null && y == null && z == null) {
      return NO_OP;
    } else {
      return new PositionOffsetInfo(x, y, z);
    }
  }

  @Override
  public @NotNull Vec3d apply(@NotNull Vec3d vec3d) {
    if (x == null && y == null && z == null) {
      return vec3d;
    } else {
      return new Vec3d(x == null ? vec3d.x : x, y == null ? vec3d.y : y, z == null ? vec3d.z : z);
    }
  }
}
