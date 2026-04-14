package pers.solid.ecmd.util;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
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
   * 将 {@link Vec3} 对象转换为 {@link CompoundTag}。如果传入的参数为 {@code null}，则返回 {@code null}。
   *
   * @param vec3d 需要转换的 {@link Vec3} 对象。
   * @return 新的 {@link CompoundTag}，包含 X、Y、Z 三个字段。
   */
  @Contract(value = "null -> null; !null->new", pure = true)
  public static @Nullable CompoundTag fromVec3d(@Nullable Vec3 vec3d) {
    if (vec3d == null) return null;
    final CompoundTag nbtCompound = new CompoundTag();
    nbtCompound.putDouble("X", vec3d.x);
    nbtCompound.putDouble("Y", vec3d.y);
    nbtCompound.putDouble("Z", vec3d.z);
    return nbtCompound;
  }

  /**
   * 将 {@link CompoundTag} 对象转换为 {@link Vec3}。如果传入的参数为 {@code null}，则返回 {@code null}。如果 NBT 中没有 X、Y、Z 字段，则这些值会作零处理。
   *
   * @param nbtCompound 需要转换为 {@link CompoundTag} 值。
   * @return 根据 NBT 数据转换而成的 {@link Vec3}。
   */
  @Contract(value = "null -> null; !null -> new", pure = true)
  public static @Nullable Vec3 toVec3d(@Nullable CompoundTag nbtCompound) {
    if (nbtCompound == null) return null;
    return new Vec3(nbtCompound.getDouble("X"), nbtCompound.getDouble("Y"), nbtCompound.getDouble("Z"));
  }

  /**
   * 将 {@link Vec3i} 对象转换为 {@link CompoundTag}。如果传入的参数为 {@code null}，则返回 {@code null}。
   *
   * @param vec3i 需要转换的 {@link Vec3i} 对象。
   * @return 新的 {@link CompoundTag}，包含 X、Y、Z 三个字段。
   * @see net.minecraft.nbt.NbtUtils#writeBlockPos(BlockPos)
   */
  @Contract(value = "null -> null; !null->new", pure = true)
  public static @Nullable CompoundTag fromVec3i(@Nullable Vec3i vec3i) {
    if (vec3i == null) return null;
    final CompoundTag nbtCompound = new CompoundTag();
    nbtCompound.putInt("X", vec3i.getX());
    nbtCompound.putInt("Y", vec3i.getY());
    nbtCompound.putInt("Z", vec3i.getZ());
    return nbtCompound;
  }

  /**
   * 将 {@link CompoundTag} 对象转换为 {@link Vec3i}。如果传入的参数为 {@code null}，则返回 {@code null}。如果 NBT 中没有 X、Y、Z 字段，则这些值会作零处理。
   *
   * @param nbtCompound 需要转换为 {@link CompoundTag} 值。
   * @return 根据 NBT 数据转换而成的 {@link Vec3i}。
   */
  @Contract(value = "null -> null; !null -> new", pure = true)
  public static @Nullable Vec3i toVec3i(@Nullable CompoundTag nbtCompound) {
    if (nbtCompound == null) return null;
    return new Vec3i(nbtCompound.getInt("X"), nbtCompound.getInt("Y"), nbtCompound.getInt("Z"));
  }

  /**
   * 将多个 {@link Tag} 的集合或迭代对象转换为 {@link ListTag}。如果传入的参数为 {@code null}，则返回 {@code null}。但是，如果传入的参数在迭代过程中返回的值不能是 {@code null}。
   *
   * @return 新的 {@link ListTag} 对象。
   */
  @Contract(value = "null -> null; !null -> new", pure = true)
  public static @Nullable ListTag fromIterable(@Nullable Iterable<Tag> elements) {
    if (elements == null) return null;
    final ListTag nbtList = new ListTag();
    Iterables.addAll(nbtList, elements);
    return nbtList;
  }

  /**
   * 将多个对象的集合或迭代对象转换为 {@link Tag}，然后再收集到 {@link ListTag} 中。如果传入的参数为 {@code null}，则返回 {@code null}。但是，如果传入的参数在迭代过程中返回的值不能是 {@code null}。
   *
   * @return 新的 {@link ListTag} 对象。
   */
  @Contract(value = "null, _ -> null; !null, _ -> new", pure = true)
  public static <T> @Nullable ListTag fromIterable(@Nullable Iterable<T> elements, Function<T, Tag> function) {
    if (elements == null) return null;
    final ListTag nbtList = new ListTag();
    Iterables.addAll(nbtList, Iterables.transform(elements, function));
    return nbtList;
  }

  /**
   * 将 {@link ListTag} 根据指定的映射转换为列表，其实现方式为 {@link Stream#toList()}。需要注意的是，NBT 列表中的各个元素必须是复合标签（{@link CompoundTag}）。
   *
   * @return 转换后的不可修改的 {@link List} 对象。
   */
  @Contract(value = "null, _ -> null; !null, _ -> new", pure = true)
  public static <T> @Nullable List<T> toImmutableList(@Nullable ListTag nbtList, java.util.function.Function<CompoundTag, T> function) {
    if (nbtList == null) return null;
    return nbtList.stream().filter(nbtElement -> nbtElement instanceof CompoundTag).map(nbtElement -> (CompoundTag) nbtElement).map(function).toList();
  }

  private static final DynamicCommandExceptionType INVALID = new DynamicCommandExceptionType(pathString -> Component.translatable("commands.data.get.invalid", pathString));

  /**
   * 如果 NBT 元素为数字，则直接返回这个值，否则抛出错误。
   */
  @Contract(value = "_, _ -> param1", pure = true)
  public static NumericTag toNumberOrThrow(Tag nbtElement, NbtPathArgument.NbtPath path) throws CommandSyntaxException {
    if (nbtElement instanceof NumericTag number) {
      return number;
    } else {
      throw INVALID.create(path.toString());
    }
  }
}
