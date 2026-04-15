package pers.solid.ecmd.util.codec;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.Util;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 一些常用 codec 及处理 codec 的类，用于进行原版无法达成的复杂处理。
 */
public final class CodecUtil {
  /**
   * 处理正则表达式的 codec，当表达式无效时，返回 error。
   */
  public static final Codec<Pattern> PATTERN = Codec.STRING.flatXmap(s -> {
    try {
      return DataResult.success(Pattern.compile(s));
    } catch (PatternSyntaxException e) {
      return DataResult.error(e::getMessage);
    }
  }, pattern -> DataResult.success(pattern.pattern()));

  /**
   * NBT 元素的 codec，部分内容使用了和 {@link Codec#PASSTHROUGH} 相似的处理方式，但是对于非 NBT 的 ops，会先转换为字符串，以确保所有 NBT 数据的类型都不会失真。
   *
   * @see net.minecraft.nbt.CompoundTag#CODEC
   */
  public static final Codec<Tag> NBT_ELEMENT = new Codec<>() {
    @Override
    public <T> DataResult<Pair<Tag, T>> decode(DynamicOps<T> ops, T input) {
      if (input instanceof Tag nbtElement) {
        return DataResult.success(Pair.of(nbtElement.copy(), input));
      }
      final DataResult<String> stringResult = Codec.STRING.parse(ops, input);
      if (stringResult.isSuccess()) {
        try {
          return DataResult.success(Pair.of(new TagParser(new StringReader(stringResult.getOrThrow())).readValue(), input));
        } catch (CommandSyntaxException e) {
          return DataResult.error(() -> "Got a string value but cannot parse as NBT: " + e.getMessage());
        }
      }
      final DataResult<Double> doubleResult = Codec.DOUBLE.parse(ops, input);
      if (doubleResult.isSuccess()) {
        return DataResult.success(Pair.of(DoubleTag.valueOf(doubleResult.getOrThrow()), input));
      }
      return DataResult.error(() -> "Cannot parse value: not a string or number");
    }

    @Override
    public <T> DataResult<T> encode(Tag input, DynamicOps<T> ops, T prefix) {
      if (input == ops.empty()) {
        return DataResult.success(prefix, Lifecycle.experimental());
      }

      final T casted;
      if (ops.empty() instanceof Tag) {
        casted = (NbtOps.INSTANCE.convertTo(ops, input));
      } else {
        casted = (ops.createString(input.toString()));
      }
      if (prefix == ops.empty()) {
        return DataResult.success(casted, Lifecycle.experimental());
      }

      final DataResult<T> toMap = ops.getMap(casted).flatMap(map -> ops.mergeToMap(prefix, map));
      return toMap.result().map(DataResult::success).orElseGet(() -> {
        final DataResult<T> toList = ops.getStream(casted).flatMap(stream -> ops.mergeToList(prefix, stream.collect(Collectors.toList())));
        return toList.result().map(DataResult::success).orElseGet(() ->
            DataResult.error(() -> "Don't know how to merge " + prefix + " and " + casted, prefix, Lifecycle.experimental())
        );
      });
    }
  };

  /**
   * NBT 数字的 codec，类似于 {@link #NBT_ELEMENT}，但是遇到非数字的值会报错。
   */
  public static final Codec<NumericTag> NBT_NUMBER = NBT_ELEMENT.comapFlatMap(nbtElement -> nbtElement instanceof NumericTag number ? DataResult.success(number) : DataResult.error(() -> "The NBT value is not a number"), Function.identity());
  public static final BiMap<String, BiConsumer<Vec3, List<? extends Entity>>> SORTER_MAP = Util.make(HashBiMap.create(), biMap -> {
    biMap.put("arbitrary", EntitySelector.ORDER_ARBITRARY);
    biMap.put("random", EntitySelectorParser.ORDER_RANDOM);
    biMap.put("furthest", EntitySelectorParser.ORDER_FURTHEST);
    biMap.put("nearest", EntitySelectorParser.ORDER_NEAREST);
  });
  /**
   * 用于序列化实体选择器中的 {@link EntitySelector#order}，即实体选择器中的 {@code sort} 参数。
   */
  public static final Codec<BiConsumer<Vec3, List<? extends Entity>>> SORTER = Codec.STRING.flatXmap(string -> SORTER_MAP.containsKey(string) ? DataResult.success(SORTER_MAP.get(string)) : DataResult.error(() -> "unknown sorter: " + string + ", which may be provided by other mods and cannot be recognized by Enhanced Commands mod")
      , biConsumer -> SORTER_MAP.inverse().containsKey(biConsumer) ? DataResult.success(SORTER_MAP.inverse().get(biConsumer)) : DataResult.error(() -> "Unknown sorter which may be provided or modified by other mods and cannot be recognized by Enhanced Commands mod"));

  /**
   * 为特定的方块，创建其属性名称的 codec，将只能读取到符合该方块的属性名称，否则会发生错误。
   */
  public static Codec<Property<?>> propertyForBlock(StateDefinition<Block, BlockState> stateManager) {
    return Codec.STRING.flatXmap(s -> {
      final Property<?> property = stateManager.getProperty(s);
      if (property == null) {
        return DataResult.error(() -> stateManager.getOwner() + " does not support property named " + s);
      }
      return DataResult.success(property);
    }, property -> DataResult.success(property.getName()));
  }

  /**
   * 为集创建 codec。虽然可以使用 {@link Codec#list(Codec)} 然后调用 {@code xmap}，但是本方法与之相比能够省略与列表的转化过程。
   *
   * @param elementCodec 集的元素的 codec。
   */
  public static <A> Codec<Set<A>> set(Codec<A> elementCodec) {
    return new SetCodec<>(elementCodec);
  }

  /**
   * 可选字段的 codec，在反序列化值时，当字段不存在时，会采用其默认值。与原版不同的时，当字段有值但是有错误时，不会采用默认值，而是抛出此错误。
   *
   * @param name         字段的名称。
   * @param elementCodec 该字段的元素的 codec。
   * @param defaultValue 字段的值不存在时，使用的默认值。
   */
  public static <A> StrictOptionalFieldCodec<A> optionalField(String name, Codec<A> elementCodec, A defaultValue) {
    return new StrictOptionalFieldCodec<>(name, elementCodec, defaultValue, true);
  }

  /**
   * 可选字段的 codec，在反序列化值时，当字段不存在时，会采用其默认值。与原版不同的时，当字段有值但是有错误时，不会采用默认值，而是抛出此错误。
   *
   * @param name            字段的名称。
   * @param elementCodec    该字段的元素的 codec。
   * @param defaultValue    字段的值不存在时，使用的默认值。
   * @param emitWhenDefault 在序列化时，当该字段的值等于默认值时，是否直接省略值。默认为 {@code true}。
   */
  public static <A> StrictOptionalFieldCodec<A> optionalField(String name, Codec<A> elementCodec, A defaultValue, boolean emitWhenDefault) {
    return new StrictOptionalFieldCodec<>(name, elementCodec, defaultValue, emitWhenDefault);
  }

  /**
   * 可选字段的 codec，在反序列化值时，当字段不存在时，会采用其 {@code Optional.empty()}。与原版不同的时，当字段有值但是有错误时，不会采用默认值，而是抛出此错误。
   *
   * @param name         字段的名称。
   * @param elementCodec 该字段的元素的 codec。
   */
  public static <A> StrictOptionalFieldCodec<Optional<A>> optionalField(String name, Codec<A> elementCodec) {
    return new StrictOptionalFieldCodec<>(name, elementCodec.xmap(Optional::of, Optional::get), Optional.empty(), true);
  }

  /**
   * <p>组合两个 codec。
   * <p>反序列化过程中，当读取到字符串值时，按照 {@code stringBased} 读取，如果读取出错，则正常抛出错误。其他情况下按照 {@code mapBased} 读取。
   * <p>序列化时，根据 {@code function} 进行转化，如果能转化（即结果不为 {@code null}），则按 {@code stringBased} 进行序列化，否则按照 {@code mapBased} 进行序列化。
   */
  public static <A, B> Codec<A> combined(Codec<B> stringBased, Codec<A> mapBased, Function<A, @Nullable B> function, Function<B, A> functionToStringBased) {
    Objects.requireNonNull(stringBased, "stringBased codec");
    Objects.requireNonNull(mapBased, "mapBased codec");
    return new Codec<>() {
      @Override
      public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        final DataResult<String> stringResult = ops.getStringValue(input);
        return stringResult.result().map(s -> stringBased.decode(ops, input).map(pair -> pair.mapFirst(functionToStringBased))).orElseGet(() -> mapBased.decode(ops, input));
      }

      @Override
      public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        // 这是考虑到，如果序列化为 NBT，考虑到 NBT 的列表含有不同类型元素可能会存在问题，因此序列化 NBT 时，使用序列化为 map；
        // 在 1.21.5 后不需要这么做。
        final B cast = ops.createBoolean(false) instanceof Tag ? null : function.apply(input);
        if (cast == null) {
          return mapBased.encode(input, ops, prefix);
        } else {
          return stringBased.encode(cast, ops, prefix);
        }
      }

      @Override
      public String toString() {
        return "combined(" + stringBased + ", " + mapBased + ")";
      }
    };
  }

  public static <A, B extends A> Codec<A> combined(Codec<B> stringBased, Codec<A> mapBased, Function<A, @Nullable B> function) {
    return combined(stringBased, mapBased, function, b -> b);
  }

  /**
   * 用于解析同时支持 ID 和标签的字符串。当读取到不以井号开头的字符串时，使用 {@code unprefixed} codec，当读取到以井号开头的字符串时，使用 {@code prefixed} codec，如果非字符串则错误。
   *
   * @param unprefixed   对不以井号开头的字符串使用的 codec。
   * @param hashPrefixed 对以井号开头的字符串使用的 codec。
   * @param <A>          不以井号开头的字符串表示的对象类型，例如 Item。
   * @param <B>          以井号开头的字符串表示的对象类型，例如 TagKey<Item>。
   */
  public static <A, B> Codec<Either<A, B>> combinedIdAndTag(Codec<A> unprefixed, Codec<B> hashPrefixed) {
    Objects.requireNonNull(unprefixed, "unprefixed codec");
    Objects.requireNonNull(hashPrefixed, "hashPrefixed codec");
    return new Codec<>() {
      @Override
      public <T> DataResult<Pair<Either<A, B>, T>> decode(DynamicOps<T> dynamicOps, T t) {
        final DataResult<String> stringValue = dynamicOps.getStringValue(t);
        return stringValue.flatMap(s -> s.startsWith("#") ? hashPrefixed.decode(dynamicOps, t).map(pair -> pair.mapFirst(Either::right)) : unprefixed.decode(dynamicOps, t).map(pair -> pair.mapFirst(Either::left)));
      }

      @Override
      public <T> DataResult<T> encode(Either<A, B> either, DynamicOps<T> dynamicOps, T t) {
        return either.map(a -> unprefixed.encode(a, dynamicOps, t), b -> hashPrefixed.encode(b, dynamicOps, t));
      }
    };
  }

  /**
   * 与 {@code Codec.LONG.optionalFieldOf} 类似，但是其类型为 {@code OptionalLong} 而非 {@code Optional<Long>}，从而在运行中避免装箱与拆箱。
   *
   * @see Codec#LONG
   * @see Codec#optionalFieldOf
   */
  public static MapCodec<OptionalLong> optionalLongFieldOf(String name) {
    return Codec.LONG.optionalFieldOf(name).xmap(ol -> ol.map(OptionalLong::of).orElseGet(OptionalLong::empty), optionalLong -> optionalLong.isEmpty() ? Optional.empty() : Optional.of(optionalLong.getAsLong()));
  }

  public static <T> MapCodec<T> unimplementedMapCodec(String message) {
    return new MapCodec<>() {
      @Override
      public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
        return Stream.empty();
      }

      @Override
      public <T1> DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input) {
        return DataResult.error(() -> message);
      }

      @Override
      public <T1> RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix) {
        return prefix.withErrorsFrom(DataResult.error(() -> message));
      }
    };
  }
}
