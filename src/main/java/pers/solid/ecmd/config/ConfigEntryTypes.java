package pers.solid.ecmd.config;

import com.mojang.brigadier.arguments.*;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TextUtil;

import java.util.HashMap;
import java.util.Map;

public final class ConfigEntryTypes {
  public static final ConfigEntryType<Boolean> BOOLEAN = ConfigEntryType.of(Codec.BOOL, PacketCodecs.BOOLEAN, TextUtil::wrapBoolean, BoolArgumentType.bool());
  public static final ConfigEntryType<Integer> INTEGER = ConfigEntryType.of(Codec.INT, PacketCodecs.INTEGER, TextUtil::literal, IntegerArgumentType.integer());
  public static final ConfigEntryType<Long> LONG = ConfigEntryType.of(Codec.LONG, PacketCodecs.LONG, TextUtil::literal, LongArgumentType.longArg());
  public static final ConfigEntryType<Float> FLOAT = ConfigEntryType.of(Codec.FLOAT, PacketCodecs.FLOAT, TextUtil::literal, FloatArgumentType.floatArg());
  public static final ConfigEntryType<Double> DOUBLE = ConfigEntryType.of(Codec.DOUBLE, PacketCodecs.DOUBLE, TextUtil::literal, DoubleArgumentType.doubleArg());

  public static final Map<Class<?>, ConfigEntryType<?>> CLASS_TO_TYPE = Util.make(new HashMap<>(), map -> {
    map.put(boolean.class, BOOLEAN);
    map.put(Boolean.class, BOOLEAN);
    map.put(int.class, INTEGER);
    map.put(Integer.class, INTEGER);
    map.put(long.class, LONG);
    map.put(Long.class, LONG);
    map.put(float.class, FLOAT);
    map.put(Float.class, FLOAT);
    map.put(double.class, DOUBLE);
    map.put(Double.class, DOUBLE);
  });

  @SuppressWarnings("unchecked")
  public static <T> @NotNull ConfigEntryType<T> fromClass(Class<T> classObject) {
    final ConfigEntryType<?> configEntryType = CLASS_TO_TYPE.get(classObject);
    if (configEntryType == null) {
      throw new IllegalArgumentException("No such config entry type for " + classObject.toString());
    } else {
      return (ConfigEntryType<T>) configEntryType;
    }
  }

  private ConfigEntryTypes() {
  }
}
