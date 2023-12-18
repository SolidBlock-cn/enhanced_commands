package pers.solid.ecmd.util;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * 与 NBT 有关的一些实用方法，包括常见的 NBT 对象与其他各类对象之间的转换。
 */
public final class NbtUtil {
  private NbtUtil() {
  }

  /**
   * 将 {@link Vec3d} 对象转换为 {@link NbtCompound}。如果传入的参数为 {@code null}，则返回 {@code null}。
   *
   * @param vec3d 需要转换的 {@link Vec3d} 对象。
   * @return 新的 {@link NbtCompound}，包含 X、Y、Z 三个字段。
   */
  @Contract(value = "null -> null; !null->new", pure = true)
  public static @Nullable NbtCompound fromVec3d(@Nullable Vec3d vec3d) {
    if (vec3d == null) return null;
    final NbtCompound nbtCompound = new NbtCompound();
    nbtCompound.putDouble("X", vec3d.x);
    nbtCompound.putDouble("Y", vec3d.y);
    nbtCompound.putDouble("Z", vec3d.z);
    return nbtCompound;
  }

  /**
   * 将 {@link NbtCompound} 对象转换为 {@link Vec3d}。如果传入的参数为 {@code null}，则返回 {@code null}。如果 NBT 中没有 X、Y、Z 字段，则这些值会作零处理。
   *
   * @param nbtCompound 需要转换为 {@link NbtCompound} 值。
   * @return 根据 NBT 数据转换而成的 {@link Vec3d}。
   */
  @Contract(value = "null -> null; !null -> new", pure = true)
  public static @Nullable Vec3d toVec3d(@Nullable NbtCompound nbtCompound) {
    if (nbtCompound == null) return null;
    return new Vec3d(nbtCompound.getDouble("X"), nbtCompound.getDouble("Y"), nbtCompound.getDouble("Z"));
  }

  /**
   * 将 {@link Vec3i} 对象转换为 {@link NbtCompound}。如果传入的参数为 {@code null}，则返回 {@code null}。
   *
   * @param vec3i 需要转换的 {@link Vec3i} 对象。
   * @return 新的 {@link NbtCompound}，包含 X、Y、Z 三个字段。
   * @see net.minecraft.nbt.NbtHelper#fromBlockPos(BlockPos)
   */
  @Contract(value = "null -> null; !null->new", pure = true)
  public static @Nullable NbtCompound fromVec3i(@Nullable Vec3i vec3i) {
    if (vec3i == null) return null;
    final NbtCompound nbtCompound = new NbtCompound();
    nbtCompound.putInt("X", vec3i.getX());
    nbtCompound.putInt("Y", vec3i.getY());
    nbtCompound.putInt("Z", vec3i.getZ());
    return nbtCompound;
  }

  /**
   * 将 {@link NbtCompound} 对象转换为 {@link Vec3i}。如果传入的参数为 {@code null}，则返回 {@code null}。如果 NBT 中没有 X、Y、Z 字段，则这些值会作零处理。
   *
   * @param nbtCompound 需要转换为 {@link NbtCompound} 值。
   * @return 根据 NBT 数据转换而成的 {@link Vec3i}。
   * @see net.minecraft.nbt.NbtHelper#toBlockPos(NbtCompound)
   */
  @Contract(value = "null -> null; !null -> new", pure = true)
  public static @Nullable Vec3i toVec3i(@Nullable NbtCompound nbtCompound) {
    if (nbtCompound == null) return null;
    return new Vec3i(nbtCompound.getInt("X"), nbtCompound.getInt("Y"), nbtCompound.getInt("Z"));
  }

  /**
   * 将多个 {@link NbtElement} 的集合或迭代对象转换为 {@link NbtList}。如果传入的参数为 {@code null}，则返回 {@code null}。但是，如果传入的参数在迭代过程中返回的值不能是 {@code null}。
   *
   * @return 新的 {@link NbtList} 对象。
   */
  @Contract(value = "null -> null; !null -> new", pure = true)
  public static @Nullable NbtList fromIterable(@Nullable Iterable<@NotNull NbtElement> elements) {
    if (elements == null) return null;
    final NbtList nbtList = new NbtList();
    Iterables.addAll(nbtList, elements);
    return nbtList;
  }

  /**
   * 将多个对象的集合或迭代对象转换为 {@link NbtElement}，然后再收集到 {@link NbtList} 中。如果传入的参数为 {@code null}，则返回 {@code null}。但是，如果传入的参数在迭代过程中返回的值不能是 {@code null}。
   *
   * @return 新的 {@link NbtList} 对象。
   */
  @Contract(value = "null, _ -> null; !null, _ -> new", pure = true)
  public static <T> @Nullable NbtList fromIterable(@Nullable Iterable<@NotNull T> elements, @NotNull Function<T, NbtElement> function) {
    if (elements == null) return null;
    final NbtList nbtList = new NbtList();
    Iterables.addAll(nbtList, Iterables.transform(elements, function));
    return nbtList;
  }

  /**
   * 将 {@link NbtList} 根据指定的映射转换为列表，其实现方式为 {@link Stream#toList()}。需要注意的是，NBT 列表中的各个元素必须是复合标签（{@link NbtCompound}）。
   *
   * @return 转换后的不可修改的 {@link List} 对象。
   */
  @Contract(value = "null, _ -> null; !null, _ -> new", pure = true)
  public static <T> @Nullable List<T> toImmutableList(@Nullable NbtList nbtList, @NotNull java.util.function.Function<@NotNull NbtCompound, T> function) {
    if (nbtList == null) return null;
    return nbtList.stream().filter(nbtElement -> nbtElement instanceof NbtCompound).map(nbtElement -> (NbtCompound) nbtElement).map(function).toList();
  }

  /**
   * 如果 NBT 元素为数字，则直接返回这个值，否则抛出错误。
   */
  @Contract(value = "_, _ -> param1", pure = true)
  public static @NotNull AbstractNbtNumber toNumberOrThrow(@NotNull NbtElement nbtElement, @NotNull NbtPathArgumentType.NbtPath path) {
    if (nbtElement instanceof AbstractNbtNumber number) {
      return number;
    } else {
      throw new CommandException(Text.translatable("commands.data.get.invalid", path.toString()));
    }
  }
}
