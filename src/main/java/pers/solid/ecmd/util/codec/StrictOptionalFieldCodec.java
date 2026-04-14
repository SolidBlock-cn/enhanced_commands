package pers.solid.ecmd.util.codec;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.OptionalFieldCodec;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.stream.Stream;

/**
 * <p>记录类型的可选字段的 codec。与原版的 {@link OptionalFieldCodec} 不同的是，反序列化过程中，如果有值但是出错，那么会直接抛出错误，而不是使用默认值（新版本 Minecraft 修复了些问题）。
 * <p>例如，如果有这样一个记录：{@code record MyRecord(int x)}，其 {@code RecordCodec} 有以下字段的 {@code codec}：
 * <pre>{@code
 * new StrictOptionalFieldCodec("x", Codec.INT, 5, false)
 * }</pre>
 * <p>那么在序列化过程中（以 NBT 为例）：
 * <ul><li>MyRecord(3) → {x: 3}</li>
 * <li>MyRecord(5) → {x: 5}</li></ul>
 * <p>而在反序列化过程中：
 * <ul><li>{x: 3} → MyRecord(3)</li>
 * <li>{x: 5} 或 {} → MyRecord(5)</li>
 * <li>{x: "string"} → 无法解析</li></ul>
 *
 * @param <A> 该字段的元素的类型。
 */
public class StrictOptionalFieldCodec<A> extends MapCodec<A> {
  /**
   * 字段的名称。
   */
  final String name;
  /**
   * 字段的元素的 codec。
   */
  private final Codec<A> elementCodec;
  /**
   * 反序列化过程中，字段不存在时所采取的默认值。
   */
  private final A defaultValue;
  /**
   * 在序列化过程中，如果值正好等于默认值，是否在序列化的结果中省略。
   */
  private final boolean emitWhenDefault;

  public StrictOptionalFieldCodec(String name, Codec<A> elementCodec, A defaultValue, boolean emitWhenDefault) {
    this.name = name;
    this.elementCodec = elementCodec;
    this.defaultValue = defaultValue;
    this.emitWhenDefault = emitWhenDefault;
  }

  @Override
  public <T> Stream<T> keys(DynamicOps<T> ops) {
    return Stream.of(ops.createString(name));
  }

  @Override
  public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
    final T value = input.get(name);
    if (value == null) {
      return DataResult.success(defaultValue);
    }
    return elementCodec.parse(ops, value);
  }

  @Override
  public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
    if (!emitWhenDefault || !defaultValue.equals(input)) {
      return prefix.add(name, elementCodec.encodeStart(ops, input));
    }
    return prefix;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StrictOptionalFieldCodec<?> that)) return false;

    return emitWhenDefault == that.emitWhenDefault && name.equals(that.name) && elementCodec.equals(that.elementCodec) && defaultValue.equals(that.defaultValue);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + elementCodec.hashCode();
    result = 31 * result + defaultValue.hashCode();
    result = 31 * result + Boolean.hashCode(emitWhenDefault);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("name", name)
        .append("elementCodec", elementCodec)
        .append("defaultValue", defaultValue)
        .append("emitWhenDefault", emitWhenDefault)
        .toString();
  }
}
