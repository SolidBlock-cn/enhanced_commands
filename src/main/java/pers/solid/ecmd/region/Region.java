package pers.solid.ecmd.region;

import com.google.common.collect.Streams;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * This interface represents a region, which contains various block positions. It has multiple types.
 */
@Unmodifiable
public interface Region extends Iterable<BlockPos>, StringIdentifiable {
  /**
   * Whether the region contains a block pos. The default implementation is converting the block pos into Vec3d, but it is recommended to override this.
   */
  @Contract(pure = true)
  default boolean contains(@NotNull Vec3i vec3i) {
    return contains(Vec3d.ofCenter(vec3i));
  }

  /**
   * Whether the region contains a position.
   */
  @Contract(pure = true)
  boolean contains(@NotNull Vec3d vec3d);

  @NotNull
  @Override
  Iterator<BlockPos> iterator();

  /**
   * Generates a stream from its iteration using {@link Streams#stream(Iterable)}.
   */
  default Stream<BlockPos> stream() {
    return Streams.stream(this);
  }

  @NotNull
  default Region moved(int count, @NotNull Direction direction) {
    return moved(direction.getVector().multiply(count));
  }

  @NotNull
  default Region moved(double count, @NotNull Direction direction) {
    return moved(Vec3d.of(direction.getVector()).multiply(count));
  }

  @NotNull
  default Region moved(@NotNull Vec3i relativePos) {
    return moved(Vec3d.of(relativePos));
  }

  @NotNull
  Region moved(@NotNull Vec3d relativePos);

  @NotNull
  Region rotated(@NotNull Vec3d center, @NotNull BlockRotation blockRotation);

  @NotNull
  Region mirrored(@NotNull Vec3d center, @NotNull Direction.Axis axis);

  @NotNull RegionType<?> getType();

  /**
   * @return The total accurate volume of the region.
   */
  @Contract(pure = true) double volume();

  /**
   * @return The <em>estimated</em> number that will be affected. It will be used to determine how should a multi-block operation be executed.
   */
  @Contract(pure = true) default long numberOfBlocksAffected() {
    return Math.round(volume());
  }

  @Override
  String asString();
}
