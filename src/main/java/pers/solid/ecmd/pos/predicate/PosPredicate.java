package pers.solid.ecmd.pos.predicate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.GeoUtil;

import java.util.function.Function;

public interface PosPredicate extends ExpressionConvertible, PosPredicateArgument {
  /**
   * 判断方块坐标是否在该区域内。其默认的实现方式是判断方块坐标的中心位置。
   */
  @Contract(pure = true)
  default boolean contains(Vec3i vec3i) {
    return contains(Vec3.atCenterOf(vec3i));
  }

  /**
   * 判断精确坐标是否在该区域内。
   */
  @Contract(pure = true)
  boolean contains(Vec3 vec3d);

  /**
   * 该区域沿指定的整数向量移动后的区域。默认情况下会将这个整数向量转换为浮点向量，但特定情况下可以修改此方法以避免使用浮点数。
   */
  default PosPredicate moved(Vec3i relativePos) {
    return moved(Vec3.atLowerCornerOf(relativePos));
  }

  /**
   * 该区域沿指定的浮点数向量移动后的区域。
   */
  default PosPredicate moved(Vec3 relativePos) {
    return transformed(vec3d -> vec3d.add(relativePos));
  }

  /**
   * 区域旋转后的区域。
   *
   * @implSpec 此区域内的所有坐标在旋转后都应该是旋转后的区域内的所有坐标，但是不需要确保旋转后的迭代顺序与之前的一致。
   */
  default PosPredicate rotated(Rotation blockRotation, Vec3 pivot) {
    return transformed(vec3d -> GeoUtil.rotate(vec3d, blockRotation, pivot));
  }

  /**
   * 区域翻转后的区域。
   *
   * @implSpec 此区域内的所有坐标在翻转后都应该是翻转后的区域内的所有坐标，但是不需要确保翻转后的迭代顺序与之前的一致。
   */
  default PosPredicate mirrored(Direction.Axis axis, Vec3 pivot) {
    return transformed(vec3d -> GeoUtil.mirror(vec3d, axis, pivot));
  }

  PosPredicate transformed(Function<Vec3, Vec3> transformation);

  @Override
  String asString();

  /**
   * 包含可能符合该位置谓词的所有坐标的最小长方体区域，用于对实体选择器进行限制。如果没有可用于限制的区域，可返回 {@code null}。
   */
  @Nullable
  default AABB minContainingBox() {
    return null;
  }

  @Override
  default PosPredicate toAbsolutePosPredicate(CommandSourceStack source) {
    return this;
  }
}
