package pers.solid.ecmd.util.bridge;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 本模组中使用的范围对象。每个范围都可能有一个最大值和最小值。与原版的 {@link MinMaxBounds} 不同的是，本对象的方法更加统一，且在不同版本之间的用法基本一致。
 */
public interface BridgeRange<T extends Comparable<T>> extends ExpressionConvertible {
  MapCodec<BridgeRange<?>> CODEC = Type.CODEC.dispatchMap("range_number_type", BridgeRange::getType, type -> type.getCodec().fieldOf("range"));

  static boolean isNextCharValid(StringReader reader) {
    char c = reader.peek();
    if ((c < '0' || c > '9') && c != '-') {
      return c == '.' && (!reader.canRead(2) || reader.peek(1) != '.');
    } else {
      return true;
    }
  }

  static <T, E1 extends Throwable, E2 extends CommandSyntaxException> @Nullable T parseNumber(StringReader reader, FailableFunction<String, T, E1> numberConverter, BiFunction<StringReader, String, E2> exceptionSupplier) throws E1, E2 {
    int cursorBeforeNumber = reader.getCursor();

    while (reader.canRead() && BridgeRange.isNextCharValid(reader)) {
      reader.skip();
    }

    final int cursorAfterNumber = reader.getCursor();
    String string = reader.getString().substring(cursorBeforeNumber, cursorAfterNumber);
    if (string.isEmpty()) {
      return null;
    }
    try {
      return numberConverter.apply(string);
    } catch (NumberFormatException ignore) {
      reader.setCursor(cursorBeforeNumber);
      throw CommandSyntaxExceptionExtension.withCursorEnd(exceptionSupplier.apply(reader, string), cursorAfterNumber);
    }
  }


  static <T extends Comparable<T>, E1 extends Throwable, E2 extends CommandSyntaxException, R extends BridgeRange<T>> R parse(StringReader reader, FailableFunction<String, T, E1> numberConverter, BiFunction<StringReader, String, E2> exceptionSupplier, BiFunction<@Nullable T, @Nullable T, @NotNull R> function) throws CommandSyntaxException, E1 {
    if (!reader.canRead()) {
      throw MinMaxBounds.ERROR_EMPTY.createWithContext(reader);
    } else {
      int cursorBeforeRange = reader.getCursor();

      @Nullable T min = parseNumber(reader, numberConverter, exceptionSupplier);
      int cursorAfterRange = reader.getCursor();
      @Nullable T max;
      if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
        reader.skip();
        reader.skip();
        max = parseNumber(reader, numberConverter, exceptionSupplier);
        cursorAfterRange = reader.getCursor();
        if (min == null && max == null) {
          reader.setCursor(cursorBeforeRange);
          throw CommandSyntaxExceptionExtension.withCursorEnd(MinMaxBounds.ERROR_EMPTY.createWithContext(reader), cursorAfterRange);
        }
      } else {
        max = min;
      }

      if (min == null && max == null) {
        reader.setCursor(cursorBeforeRange);
        throw CommandSyntaxExceptionExtension.withCursorEnd(MinMaxBounds.ERROR_EMPTY.createWithContext(reader), cursorAfterRange);
      } else if (min != null && max != null && min.compareTo(max) > 0) {
        reader.setCursor(cursorBeforeRange);
        throw CommandSyntaxExceptionExtension.withCursorEnd(MinMaxBounds.ERROR_SWAPPED.createWithContext(reader), cursorAfterRange);
      } else {
        return function.apply(min, max);
      }
    }
  }

  static <T extends Comparable<T>, R extends BridgeRange<T>> Codec<R> createMapBasedCodec(Codec<T> valueCodec, BiFunction<Optional<T>, Optional<T>, R> instanceFunction) {
    return RecordCodecBuilder.create(
        instance -> instance.group(valueCodec.optionalFieldOf("min").forGetter(BridgeRange::optionalMin), valueCodec.optionalFieldOf("max").forGetter(BridgeRange::optionalMax))
            .apply(instance, instanceFunction)
    );
  }

  static <T extends Comparable<T>, R extends BridgeRange<T>> Codec<R> createCodec(Codec<T> valueCodec, BiFunction<Optional<T>, Optional<T>, R> instanceFunction) {
    return Codec.either(createMapBasedCodec(valueCodec, instanceFunction), valueCodec)
        .xmap(either -> either.map(Function.identity(), value -> instanceFunction.apply(Optional.of(value), Optional.of(value))), range -> {
          Optional<T> optional = range.getConstantValue();
          return optional.<Either<R, T>>map(Either::right).orElseGet(() -> Either.left(range));
        });
  }

  T getMin();

  T getMax();

  boolean test(T value);

  boolean isDummy();

  boolean isExact();

  Optional<T> getConstantValue();

  private Optional<T> optionalMin() {
    return Optional.ofNullable(getMin());
  }

  private Optional<T> optionalMax() {
    return Optional.ofNullable(getMax());
  }

  @Override
  default @NotNull String asString() {
    final T min = getMin();
    if (isExact()) {
      return min.toString();
    } else {
      final T max = getMax();
      return (min == null ? "" : min.toString()) + ".." + (max == null ? "" : max.toString());
    }
  }

  Type getType();

  enum Type implements StringRepresentable {
    // 这里没有将 codec 作为字段存储，而是在 getCodec 方法中通过 switch 语句获取，是因为如果存储为字段，则出现过读取为 null 的问题。
    FLOAT("float"), INT("int"), DOUBLE("double");

    public static final StringIdentifiableCodec<Type> CODEC = StringIdentifiableCodec.create(values());

    private final String s;

    Type(String s) {
      this.s = s;
    }

    public Codec<? extends BridgeRange<?>> getCodec() {
      return switch (this) {
        case FLOAT -> BridgeFloatRange.CODEC;
        case INT -> BridgeIntRange.CODEC;
        case DOUBLE -> BridgeDoubleRange.CODEC;
      };
    }

    @Override
    public String getSerializedName() {
      return s;
    }
  }
}
