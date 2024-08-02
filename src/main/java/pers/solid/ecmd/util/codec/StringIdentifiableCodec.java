package pers.solid.ecmd.util.codec;

import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * 解析并处理枚举（需继续 {@link StringIdentifiable} 的 codec，同时也能够直接与字符串进行转换。与原版的 {@link StringIdentifiable.EnumCodec} 不同的是，本类没有弃用，可以正常使用，且在不同版本之间能够保持一致。
 *
 * @param <E> 枚举类型
 */
public class StringIdentifiableCodec<E extends StringIdentifiable> extends StringIdentifiable.BasicCodec<E> {
  private final Function<String, E> idToIdentifiable;

  private StringIdentifiableCodec(E[] values, Function<String, E> idToIdentifiable) {
    super(values, idToIdentifiable, enum_ -> ((Enum<?>) enum_).ordinal());
    this.idToIdentifiable = idToIdentifiable;
  }

  public static <E extends StringIdentifiable> StringIdentifiableCodec<E> create(E[] values) {
    return create(values, Function.identity());
  }

  public static <E extends StringIdentifiable> StringIdentifiableCodec<E> create(E[] values, Function<String, String> valueNameTransformer) {
    Function<String, E> function = StringIdentifiable.createMapper(values, valueNameTransformer);
    return new StringIdentifiableCodec<>(values, function);
  }

  public @Nullable E byId(@Nullable String id) {
    return this.idToIdentifiable.apply(id);
  }

  public E byId(@Nullable String id, E fallback) {
    return Objects.requireNonNullElse(this.byId(id), fallback);
  }

  public @NotNull <T extends Throwable> E byIdOrThrow(@NotNull String id, Function<String, T> exceptionSupplier) throws T {
    final E value = byId(id);
    if (value == null) {
      throw exceptionSupplier.apply(id);
    }
    return value;
  }
}
