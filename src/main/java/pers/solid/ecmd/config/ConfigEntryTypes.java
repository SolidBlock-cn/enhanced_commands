package pers.solid.ecmd.config;

import com.mojang.brigadier.arguments.*;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.network.codec.ByteBufCodecs;
import pers.solid.ecmd.util.TextUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 本模组使用的配置项的类型。
 */
public final class ConfigEntryTypes {
  public static final ConfigEntryType<Boolean> BOOLEAN = ConfigEntryType.of(Codec.BOOL, ByteBufCodecs.BOOL, TextUtil::wrapBoolean, BoolArgumentType.bool());
  public static final ConfigEntryType<Integer> INTEGER = ConfigEntryType.of(Codec.INT, ByteBufCodecs.INT, TextUtil::literal, IntegerArgumentType.integer());
  public static final ConfigEntryType<Long> LONG = ConfigEntryType.of(Codec.LONG, ByteBufCodecs.LONG, TextUtil::literal, LongArgumentType.longArg());
  public static final ConfigEntryType<Float> FLOAT = ConfigEntryType.of(Codec.FLOAT, ByteBufCodecs.FLOAT, TextUtil::literal, FloatArgumentType.floatArg());
  public static final ConfigEntryType<Double> DOUBLE = ConfigEntryType.of(Codec.DOUBLE, ByteBufCodecs.DOUBLE, TextUtil::literal, DoubleArgumentType.doubleArg());

  /**
   * 类到配置项类型对象的映射，主要用于 {@link ConfigEntryType#fromClass}。
   */
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

  private ConfigEntryTypes() {
  }
}
