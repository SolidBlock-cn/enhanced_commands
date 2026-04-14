package pers.solid.ecmd.util.codec;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.enums.CommandEnumType;

import java.util.function.Function;

/**
 * <p>解析并处理枚举（需继承 {@link StringRepresentable}）的 codec，同时也能够直接与字符串进行转换。与原版的 {@link StringRepresentable.EnumCodec} 不同的是，本类没有弃用，可以正常使用，且避免了不同游戏版本之间的差异。
 * <p>本类实现了 {@link EnumCodec}，可直接用于 {@link CommandEnumType}。
 *
 * @param <E> 枚举类型
 */
public class StringIdentifiableCodec<E extends Enum<E> & StringRepresentable> extends StringRepresentable.StringRepresentableCodec<E> implements EnumCodec<E> {
  private final Function<String, E> idToIdentifiable;

  private StringIdentifiableCodec(E[] values, Function<String, E> idToIdentifiable) {
    super(values, idToIdentifiable, Enum::ordinal);
    this.idToIdentifiable = idToIdentifiable;
  }

  /**
   * 根据枚举的 {@code values()} 方法创建，通常是枚举常量的数组形式。
   */
  public static <E extends Enum<E> & StringRepresentable> StringIdentifiableCodec<E> create(E[] values) {
    return create(values, Function.identity());
  }

  /**
   * 根据枚举的 {@code values()} 方法创建，通常是枚举常量的数组形式，并转化由 {@link StringRepresentable#getSerializedName()} 返回的字符串 id。
   */
  public static <E extends Enum<E> & StringRepresentable> StringIdentifiableCodec<E> create(E[] values, Function<String, String> valueNameTransformer) {
    Function<String, E> function = StringRepresentable.createNameLookup(values, valueNameTransformer);
    return new StringIdentifiableCodec<>(values, function);
  }

  @Override
  public @Nullable E byId(@Nullable String id) {
    return this.idToIdentifiable.apply(id);
  }

  @Override
  public String asString(E value) {
    return value.getSerializedName();
  }
}
