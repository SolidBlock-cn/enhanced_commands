package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * 此类对应于 {@link EntitySelector#position}，但由于该字段为 {@code Function}，无法直接进行序列化和反序列化，因此需要在解析实体选择器时，也就是在 {@link EntitySelectorParser#getSelector()} 中，利用 mixin 将其 x、y、z 存储在字段中。
 *
 * @param x 对应实体选择器的 x 参数。
 * @param y 对应实体选择器的 y 参数。
 * @param z 对应实体选择器的 z 参数。
 */
public record PositionOffsetInfo(@Nullable Double x, @Nullable Double y, @Nullable Double z) implements Function<Vec3, Vec3> {
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
  public @NotNull Vec3 apply(@NotNull Vec3 vec3d) {
    if (x == null && y == null && z == null) {
      return vec3d;
    } else {
      return new Vec3(x == null ? vec3d.x : x, y == null ? vec3d.y : y, z == null ? vec3d.z : z);
    }
  }
}
