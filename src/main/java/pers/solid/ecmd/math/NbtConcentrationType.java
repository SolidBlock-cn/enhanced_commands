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

import java.util.Iterator;
import java.util.List;

public enum NbtConcentrationType implements StringIdentifiable {
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
  LIST("list") {
    @Override
    public NbtElement concentrate(Iterable<? extends NbtElement> elements, Random random) throws CommandSyntaxException {
      final NbtList list = new NbtList();
      Iterables.addAll(list, elements);
      return list;
    }
  },
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
  };

  public static final SimpleCommandExceptionType NO_DATA_TO_BE_CONCENTRATED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.nbt_concentration_type.no_data"));
  public static final Dynamic2CommandExceptionType MIXED_TYPE = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.nbt_concentration_type.mixed_type", a, b));
  public static final DynamicCommandExceptionType UNSUPPORTED_TYPE = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.nbt_concentration_type.unsupported_type", o));
  public static final StringIdentifiable.Codec<NbtConcentrationType> CODEC = StringIdentifiable.createCodec(NbtConcentrationType::values);

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
