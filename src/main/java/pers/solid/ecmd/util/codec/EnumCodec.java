package pers.solid.ecmd.util.codec;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.enums.CommandEnumType;

import java.util.Objects;
import java.util.function.Function;

/**
 * <p>用于处理枚举与字符串的转化的类的接口。该接口提供如下方法：</p>
 * <ul>
 *   <li>枚举到字符串：{@link #byId}、{@link #byIdOrThrow}。</li>
 *   <li>字符串到枚举：{@link #asString}</li>
 * </ul>
 * <p>需要注意的是，本类并不直接继承 Mojang 的 {@link Codec} 类，也不要求枚举直接继承 {@link StringIdentifiable}。对于继承了 {@link StringIdentifiable} 的枚举，可直接使用 {@link StringIdentifiableCodec}，这个类也继承了 {@link Codec} 类。而对于没有继承 {@link StringIdentifiableCodec} 的枚举，可直接使用 {@link Simple}，并通过 lambda 的方式指定枚举与字符串之间的转化。</p>
 * <p>本接口可用于 {@link CommandEnumType}，从而处理命令中的枚举与字符串的转化。</p>
 *
 * @param <E> 枚举的类型，通常（但不一定）继承 {@link StringIdentifiable}。
 */
public interface EnumCodec<E extends Enum<E>> {
  /**
   * 通过字符串 id 获取枚举常量，如果这个字符串 id 对应的枚举常量不存在，则返回 {@code null}。
   *
   * @param id 枚举常量的字符串 id
   * @return 指定的枚举常量，或者在字符串 id 对应的枚举常量不存在时为 {@code null}
   */
  @Nullable
  E byId(@Nullable String id);

  /**
   * 通过字符串 id 获取枚举常量，如果这个字符串 id 对应的枚举常量不存在，则返回 {@code fallback}。
   *
   * @param id       枚举常量的字符串 id
   * @param fallback 字符串 id 对应的枚举常量不存在时返回的值
   * @return 指定的枚举常量，或者在字符串 id 对应的枚举常量不存在时为 {@code fallback} 的值
   */
  default @NotNull E byId(@Nullable String id, E fallback) {
    return Objects.requireNonNullElse(this.byId(id), fallback);
  }

  /**
   * 通过字符串 id 获取枚举常量，如果这个字符串 id 对应的枚举常量不存在，则抛出错误。
   *
   * @param id                枚举常量的字符串 id
   * @param exceptionSupplier 字符串 id 对应的枚举常量不存在时抛出的错误、
   * @return 枚举常量，成功执行时一定非 {@code null}
   * @throws T 字符串 id 对应的枚举常量不存在
   */
  default <T extends Throwable> @NotNull E byIdOrThrow(@NotNull String id, Function<String, T> exceptionSupplier) throws T {
    final E value = byId(id);
    if (value == null) {
      throw exceptionSupplier.apply(id);
    }
    return value;
  }

  /**
   * 获取指定的枚举常量对应的字符串 id。
   *
   * @param value 枚举常量
   * @return 枚举常量对应的字符串 id
   */
  @NotNull
  String asString(@NotNull E value);

  /**
   * 对于没有继承 {@link StringIdentifiable} 也不需要 {@link Codec} 的枚举，可以利用此类创建一个简单的，需要直接提供枚举常量与字符串 id 之间的转化形式（通常是 lambda 的形式）。
   */
  record Simple<E extends Enum<E>>(Function<@Nullable String, @Nullable E> byId, Function<@NotNull E, @NotNull String> toId) implements EnumCodec<E> {
    @Override
    public @Nullable E byId(@Nullable String id) {
      return byId.apply(id);
    }

    @Override
    public @NotNull String asString(@NotNull E value) {
      return toId.apply(value);
    }
  }
}
