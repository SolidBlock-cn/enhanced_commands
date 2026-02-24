package pers.solid.ecmd.math;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.Iterator;
import java.util.List;

/**
 * <p>NBT 聚合类型，指定多个 NBT 数据如何转化为单个 NBT。
 * <p>很多情况下，例如 {@code /nbt} 命令，一次获取了多个 NBT，如果需要将结果转化为单个 NBT，则需要指定聚合类型。
 */
public enum NbtConcentrationType implements StringRepresentable {
  /**
   * 取所得数据的第一个值。如果没有数据，则抛出异常。
   */
  FIRST("first") {
    @Override
    public Tag concentrate(Iterable<? extends Tag> elements, RandomSource random) throws CommandSyntaxException {
      final Iterator<? extends Tag> iterator = elements.iterator();
      if (!iterator.hasNext()) {
        throw NO_DATA_TO_BE_CONCENTRATED.create();
      }
      return iterator.next();
    }
  },
  /**
   * 取所得数据的最后一个值。如果没有数据，则抛出异常。
   */
  LAST("last") {
    @Override
    public Tag concentrate(Iterable<? extends Tag> elements, RandomSource random) throws CommandSyntaxException {
      try {
        return Iterables.getLast(elements);
      } catch (Exception e) {
        throw NO_DATA_TO_BE_CONCENTRATED.create();
      }
    }
  },
  /**
   * 取所得数据的最小值。只有当所有数据都是数字或都是字符串时，才能生效，否则会抛出错误。
   */
  MIN("min") {
    @Override
    public Tag concentrate(Iterable<? extends Tag> elements, RandomSource random) throws CommandSyntaxException {
      final Iterator<? extends Tag> iterator = elements.iterator();
      if (!iterator.hasNext()) {
        throw NO_DATA_TO_BE_CONCENTRATED.create();
      }
      final Tag first = iterator.next();
      final String name1 = first.getType().getPrettyName();
      if (first instanceof NumericTag number) {
        NumericTag min = number;
        while (iterator.hasNext()) {
          final Tag next1 = iterator.next();
          if (!(next1 instanceof NumericTag nextNumber)) {
            throw MIXED_TYPE.create(name1, next1.getType().getPrettyName());
          }
          if (nextNumber.getAsDouble() < min.getAsDouble()) {
            min = nextNumber;
          }
        }
        return min;
      } else if (first instanceof StringTag nbtString) {
        StringTag min = nbtString;
        while (iterator.hasNext()) {
          final Tag next1 = iterator.next();
          if (!(next1 instanceof StringTag nextNumber)) {
            throw MIXED_TYPE.create(name1, next1.getType().getPrettyName());
          }
          if (nextNumber.getAsString().compareTo(min.getAsString()) < 0) {
            min = nextNumber;
          }
        }
        return min;
      } else if (iterator.hasNext()) {
        // 此情况说明还有更多元素
        throw UNSUPPORTED_TYPE.create(first.getType().getPrettyName());
      } else {
        return first;
      }
    }
  },
  /**
   * 取所得数据的最大值。只有当所有数据都是数字或都是字符串时，才能生效，否则会抛出错误。
   */
  MAX("max") {
    @Override
    public Tag concentrate(Iterable<? extends Tag> elements, RandomSource random) throws CommandSyntaxException {
      final Iterator<? extends Tag> iterator = elements.iterator();
      if (!iterator.hasNext()) {
        throw NO_DATA_TO_BE_CONCENTRATED.create();
      }
      final Tag first = iterator.next();
      final String name1 = first.getType().getPrettyName();
      if (first instanceof NumericTag number) {
        NumericTag min = number;
        while (iterator.hasNext()) {
          final Tag next1 = iterator.next();
          if (!(next1 instanceof NumericTag nextNumber)) {
            throw MIXED_TYPE.create(name1, next1.getType().getPrettyName());
          }
          if (nextNumber.getAsDouble() > min.getAsDouble()) {
            min = nextNumber;
          }
        }
        return min;
      } else if (first instanceof StringTag nbtString) {
        StringTag min = nbtString;
        while (iterator.hasNext()) {
          final Tag next1 = iterator.next();
          if (!(next1 instanceof StringTag nextNumber)) {
            throw MIXED_TYPE.create(name1, next1.getType().getPrettyName());
          }
          if (nextNumber.getAsString().compareTo(min.getAsString()) > 0) {
            min = nextNumber;
          }
        }
        return min;
      } else if (iterator.hasNext()) {
        // 此情况说明还有更多元素
        throw UNSUPPORTED_TYPE.create(first.getType().getPrettyName());
      } else {
        return first;
      }
    }
  },
  /**
   * 将所有数据组合成列表。如果这些数据混合有多个类型的数据，则会抛出错误。如果没有数据，则正常返回空列表。
   */
  LIST("list") {
    @Override
    public Tag concentrate(Iterable<? extends Tag> elements, RandomSource random) {
      final ListTag list = new ListTag();
      Iterables.addAll(list, elements);
      return list;
    }
  },
  /**
   * 取所有数据中的随机值。如果没有数据，则抛出异常。
   */
  RANDOM("random") {
    @Override
    public Tag concentrate(Iterable<? extends Tag> elements, RandomSource random) throws CommandSyntaxException {
      if (Iterables.isEmpty(elements)) {
        throw NO_DATA_TO_BE_CONCENTRATED.create();
      }
      final List<? extends Tag> list;
      if (elements instanceof List<? extends Tag> list1) {
        list = list1;
      } else {
        list = ImmutableList.copyOf(elements);
      }
      return list.get(random.nextInt(list.size()));
    }
  },
  /**
   * 如果所有数据只有一个，则正常返回该数据，否则抛出异常。在一些命令（如 {@code /nbt}）会有特殊处理，以直接在命令反馈中显示多个数据。
   */
  ALL("all") {
    @Override
    public Tag concentrate(Iterable<? extends Tag> elements, RandomSource random) throws CommandSyntaxException {
      final Iterator<? extends Tag> iterator = elements.iterator();
      if (iterator.hasNext()) {
        final Tag next = iterator.next();
        if (!iterator.hasNext()) {
          return next;
        }
      }
      throw CANNOT_CONCENTRATE.create();
    }
  };

  public static final SimpleCommandExceptionType NO_DATA_TO_BE_CONCENTRATED = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_concentration_type.no_data"));
  public static final Dynamic2CommandExceptionType MIXED_TYPE = new Dynamic2CommandExceptionType((a, b) -> Component.translatable("enhanced_commands.nbt_concentration_type.mixed_type", a, b));
  public static final DynamicCommandExceptionType UNSUPPORTED_TYPE = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.nbt_concentration_type.unsupported_type", o));
  public static final SimpleCommandExceptionType CANNOT_CONCENTRATE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_concentration_type.not_supported"));
  public static final StringIdentifiableCodec<NbtConcentrationType> CODEC = StringIdentifiableCodec.create(NbtConcentrationType.values());

  private final String name;

  NbtConcentrationType(String name) {
    this.name = name;
  }

  @Override
  public @NotNull String getSerializedName() {
    return name;
  }

  public MutableComponent getDisplayName() {
    return Component.translatable("enhanced_commands.concentration_type." + name);
  }

  public abstract Tag concentrate(Iterable<? extends Tag> elements, RandomSource random) throws CommandSyntaxException;
}
