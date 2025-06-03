package pers.solid.ecmd.math;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.random.Random;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.Iterator;
import java.util.List;

/**
 * <p>NBT 聚合类型，指定多个 NBT 数据如何转化为单个 NBT。
 * <p>很多情况下，例如 {@code /nbt} 命令，一次获取了多个 NBT，如果需要将结果转化为单个 NBT，则需要指定聚合类型。
 */
public enum NbtConcentrationType implements StringIdentifiable {
  /**
   * 取所得数据的第一个值。如果没有数据，则抛出异常。
   */
  FIRST("first") {
    @Override
    public NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) throws CommandSyntaxException {
      final Iterator<? extends NbtElement> iterator = elements.iterator();
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
    public NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) throws CommandSyntaxException {
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
    public NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) throws CommandSyntaxException {
      final Iterator<? extends NbtElement> iterator = elements.iterator();
      if (!iterator.hasNext()) {
        throw NO_DATA_TO_BE_CONCENTRATED.create();
      }
      final NbtElement first = iterator.next();
      final String name1 = first.getNbtType().getCommandFeedbackName();
      if (first instanceof AbstractNbtNumber number) {
        AbstractNbtNumber min = number;
        while (iterator.hasNext()) {
          final NbtElement next1 = iterator.next();
          if (!(next1 instanceof AbstractNbtNumber nextNumber)) {
            throw MIXED_TYPE.create(name1, next1.getNbtType().getCommandFeedbackName());
          }
          if (nextNumber.doubleValue() < min.doubleValue()) {
            min = nextNumber;
          }
        }
        return min;
      } else if (first instanceof NbtString nbtString) {
        NbtString min = nbtString;
        while (iterator.hasNext()) {
          final NbtElement next1 = iterator.next();
          if (!(next1 instanceof NbtString nextNumber)) {
            throw MIXED_TYPE.create(name1, next1.getNbtType().getCommandFeedbackName());
          }
          if (nextNumber.asString().compareTo(min.asString()) < 0) {
            min = nextNumber;
          }
        }
        return min;
      } else if (iterator.hasNext()) {
        // 此情况说明还有更多元素
        throw UNSUPPORTED_TYPE.create(first.getNbtType().getCommandFeedbackName());
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
    public NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) throws CommandSyntaxException {
      final Iterator<? extends NbtElement> iterator = elements.iterator();
      if (!iterator.hasNext()) {
        throw NO_DATA_TO_BE_CONCENTRATED.create();
      }
      final NbtElement first = iterator.next();
      final String name1 = first.getNbtType().getCommandFeedbackName();
      if (first instanceof AbstractNbtNumber number) {
        AbstractNbtNumber min = number;
        while (iterator.hasNext()) {
          final NbtElement next1 = iterator.next();
          if (!(next1 instanceof AbstractNbtNumber nextNumber)) {
            throw MIXED_TYPE.create(name1, next1.getNbtType().getCommandFeedbackName());
          }
          if (nextNumber.doubleValue() > min.doubleValue()) {
            min = nextNumber;
          }
        }
        return min;
      } else if (first instanceof NbtString nbtString) {
        NbtString min = nbtString;
        while (iterator.hasNext()) {
          final NbtElement next1 = iterator.next();
          if (!(next1 instanceof NbtString nextNumber)) {
            throw MIXED_TYPE.create(name1, next1.getNbtType().getCommandFeedbackName());
          }
          if (nextNumber.asString().compareTo(min.asString()) > 0) {
            min = nextNumber;
          }
        }
        return min;
      } else if (iterator.hasNext()) {
        // 此情况说明还有更多元素
        throw UNSUPPORTED_TYPE.create(first.getNbtType().getCommandFeedbackName());
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
    public NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) {
      final NbtList list = new NbtList();
      Iterables.addAll(list, elements);
      return list;
    }
  },
  /**
   * 取所有数据中的随机值。如果没有数据，则抛出异常。
   */
  RANDOM("random") {
    @Override
    public NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) throws CommandSyntaxException {
      if (Iterables.isEmpty(elements)) {
        throw NO_DATA_TO_BE_CONCENTRATED.create();
      }
      final List<? extends NbtElement> list;
      if (elements instanceof List<? extends NbtElement> list1) {
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
    public NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) throws CommandSyntaxException {
      final Iterator<? extends NbtElement> iterator = elements.iterator();
      if (iterator.hasNext()) {
        final NbtElement next = iterator.next();
        if (!iterator.hasNext()) {
          return next;
        }
      }
      throw CANNOT_CONCENTRATE.create();
    }
  };

  public static final SimpleCommandExceptionType NO_DATA_TO_BE_CONCENTRATED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.nbt_concentration_type.no_data"));
  public static final Dynamic2CommandExceptionType MIXED_TYPE = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.nbt_concentration_type.mixed_type", a, b));
  public static final DynamicCommandExceptionType UNSUPPORTED_TYPE = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.nbt_concentration_type.unsupported_type", o));
  public static final SimpleCommandExceptionType CANNOT_CONCENTRATE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.nbt_concentration_type.not_supported"));
  public static final StringIdentifiableCodec<NbtConcentrationType> CODEC = StringIdentifiableCodec.create(NbtConcentrationType.values());

  private final String name;

  NbtConcentrationType(String name) {
    this.name = name;
  }

  @Override
  public String asString() {
    return name;
  }

  public MutableText getDisplayName() {
    return Text.translatable("enhanced_commands.concentration_type." + name);
  }

  public abstract NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) throws CommandSyntaxException;
}
