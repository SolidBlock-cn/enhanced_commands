package pers.solid.ecmd.util.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 一些常用 codec 及处理 codec 的类，用于进行原版无法达成的复杂处理。。
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

  private CodecUtil() {
  }

  /**
   * 为特定的方块，创建其属性名称的 codec，将只能读取到符合该方块的属性名称，否则会发生错误。
   */
  public static Codec<Property<?>> propertyForBlock(StateManager<Block, BlockState> stateManager) {
    return Codec.STRING.flatXmap(s -> {
      final Property<?> property = stateManager.getProperty(s);
      if (property == null) {
        return DataResult.error(() -> stateManager.getOwner() + " does not support property named " + s);
      }
      return DataResult.success(property);
    }, property -> DataResult.success(property.getName()));
  }

  /**
   * 为集创建 codec。类似于 {@link Codec#list(Codec)} 然后调用 {@code xmap}，但是会省略与列表的转化过程。
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
  public static <A> StrictOptionalFieldCodec<A> optionalField(@NotNull String name, @NotNull Codec<A> elementCodec, @NotNull A defaultValue) {
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
  public static <A> StrictOptionalFieldCodec<A> optionalField(@NotNull String name, @NotNull Codec<A> elementCodec, @NotNull A defaultValue, boolean emitWhenDefault) {
    return new StrictOptionalFieldCodec<>(name, elementCodec, defaultValue, emitWhenDefault);
  }

  /**
   * 可选字段的 codec，在反序列化值时，当字段不存在时，会采用其 {@code Optional.empty()}。与原版不同的时，当字段有值但是有错误时，不会采用默认值，而是抛出此错误。
   *
   * @param name         字段的名称。
   * @param elementCodec 该字段的元素的 codec。
   */
  public static <A> StrictOptionalFieldCodec<Optional<A>> optionalField(@NotNull String name, @NotNull Codec<A> elementCodec) {
    return new StrictOptionalFieldCodec<>(name, elementCodec.xmap(Optional::of, Optional::get), Optional.empty(), true);
  }

  /**
   * <p>组合两个 codec。
   * <p>反序列化过程中，当读取到字符串值时，按照 {@code stringBased} 读取，如果读取出错，则正常抛出错误。其他情况下按照 {@code mapBased} 读取。
   * <p>序列化时，根据 {@code function} 进行转化，如果能转化（即结果不为 {@code null}），则按 {@code stringBased} 进行序列化，否则按照 {@code mapBased} 进行序列化。
   */
  public static <A, B extends A> Codec<A> combined(Codec<B> stringBased, Codec<A> mapBased, Function<A, @Nullable B> function) {
    return new Codec<>() {
      @Override
      public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        final DataResult<String> stringResult = ops.getStringValue(input);
        return stringResult.result().map(s -> stringBased.decode(ops, input).<Pair<A, T>>map(pair -> pair.mapFirst(Function.identity()))).orElseGet(() -> mapBased.decode(ops, input));
      }

      @Override
      public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        final B cast = function.apply(input);
        if (cast == null) {
          return mapBased.encode(input, ops, prefix);
        } else {
          return stringBased.encode(cast, ops, prefix);
        }
      }

      @Override
      public String toString() {
        return "combined(" + stringBased.toString() + ", " + mapBased.toString() + ")";
      }
    };
  }

  /**
   * 与 {@code Codec.INT.optionalFieldOf} 类似，但是其类型为 {@code OptionalInt} 而非 {@code Optional<Integer>}，从而减少不必要的装箱与拆箱。
   *
   * @see Codec#INT
   * @see Codec#optionalFieldOf
   */
  public MapCodec<OptionalInt> optionalIntFieldOf(String name) {
    return Codec.INT.optionalFieldOf(name).xmap(ol -> ol.map(OptionalInt::of).orElseGet(OptionalInt::empty), optionalInt -> optionalInt.isEmpty() ? Optional.empty() : Optional.of(optionalInt.getAsInt()));
  }

  /**
   * 与 {@code Codec.LONG.optionalFieldOf} 类似，但是其类型为 {@code OptionalLong} 而非 {@code Optional<Long>}，从而减少不必要的装箱与拆箱。
   *
   * @see Codec#LONG
   * @see Codec#optionalFieldOf
   */
  public static MapCodec<OptionalLong> optionalLongFieldOf(String name) {
    return Codec.LONG.optionalFieldOf(name).xmap(ol -> ol.map(OptionalLong::of).orElseGet(OptionalLong::empty), optionalLong -> optionalLong.isEmpty() ? Optional.empty() : Optional.of(optionalLong.getAsLong()));
  }
}
