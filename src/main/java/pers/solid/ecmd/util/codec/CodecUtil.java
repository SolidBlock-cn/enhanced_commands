package pers.solid.ecmd.util.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 包含一些常用 codec 的类。
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

  public static Codec<Property<?>> propertyForBlock(StateManager<Block, BlockState> stateManager) {
    return Codec.STRING.flatXmap(s -> {
      final Property<?> property = stateManager.getProperty(s);
      if (property == null) {
        return DataResult.error(() -> stateManager.getOwner() + " does not support property named " + s);
      }
      return DataResult.success(property);
    }, property -> DataResult.success(property.getName()));
  }

  public static <A> Codec<Set<A>> set(Codec<A> elementCodec) {
    return new SetCodec<>(elementCodec);
  }

  public static <A> StrictOptionalFieldCodec<A> optionalField(@NotNull String name, @NotNull Codec<A> elementCodec, @NotNull A defaultValue) {
    return new StrictOptionalFieldCodec<>(name, elementCodec, defaultValue, true);
  }

  public static <A> StrictOptionalFieldCodec<A> optionalField(@NotNull String name, @NotNull Codec<A> elementCodec, @NotNull A defaultValue, boolean emitWhenDefault) {
    return new StrictOptionalFieldCodec<>(name, elementCodec, defaultValue, emitWhenDefault);
  }

  public static <A> StrictOptionalFieldCodec<Optional<A>> optionalField(@NotNull String name, @NotNull Codec<A> elementCodec) {
    return new StrictOptionalFieldCodec<>(name, elementCodec.xmap(Optional::of, Optional::get), Optional.empty(), true);
  }

  public static <A> Codec<A> combined(Codec<? extends A> stringBased, Codec<A> mapBased) {
    return Codec.of(mapBased, new Decoder<>() {
      @Override
      public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        final DataResult<String> stringResult = ops.getStringValue(input);
        return stringResult.result().map(s -> stringBased.decode(ops, input).<Pair<A, T>>map(pair -> pair.mapFirst(Function.identity()))).orElseGet(() -> mapBased.decode(ops, input));
      }

      @Override
      public String toString() {
        return stringBased + " or " + mapBased;
      }
    }, "combined(" + stringBased.toString() + ", " + mapBased.toString() + ")");
  }
}
