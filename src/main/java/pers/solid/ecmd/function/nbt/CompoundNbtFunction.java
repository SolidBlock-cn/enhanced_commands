package pers.solid.ecmd.function.nbt;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在 NBT 复合标签（目标标签）中加入另一个 NBT 复合标签（源标签）的内容。相同的键会被覆盖。目标标签已有但是源标签中没有的键不会受到影响。在加入目标标签中不存在的键时，其值相当于函数对 null 的值。例如：
 * <pre>
 *   {a: b, c: d}({a: B, e: F}) = {a: b, c: d, e: F}
 *   {a: b, c: {d: e}}({c: {f: g}}) = {a: b, c: {d: e, f: g}}
 *   {}({a: b, c: d}) = {a: b, c: d}
 *   {a: b, c: d}({}) = {a: b, c: d}
 * </pre>
 * 如果目标不是 NBT 复合标签，则直接返回源 NBT 复合标签的值。例如：
 * <pre>
 *   {a: b}([1, 2, 3]) = {a: b}
 *   {a: b}("string") = {a: b}
 * </pre>
 * 在键前加上 {@code -}（横线）和一个空格且不提供值，可以删除一个键。例如：
 * <pre>
 *   {- a, - c}({a: b, c: d}) = {}
 * </pre>
 * 如果前面是 {@code =}（等号），则禁用合并功能：
 * <pre>
 *   ={a: b}({c: d}) = {a: b}
 * </pre>
 * 不允许重复键，重复键会被覆盖。
 *
 * @param allowsMerge 是否允许对 NBT 复合标签进行合并
 */
public record CompoundNbtFunction(Map<String, @Nullable NbtFunction> source, boolean allowsMerge) implements NbtFunction {
  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return (allowsMerge ? (requirePrefix ? ": " : "") : "= ") + "{" + source.entrySet().stream().map(entry -> {
      final String key = entry.getKey();
      final NbtFunction value = entry.getValue();
      if (value == null) {
        return "- " + key;
      } else {
        final String valueAsString = value.asString(true);
        return key + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
      }
    }).collect(Collectors.joining(", ")) + "}";
  }

  @Override
  public @NotNull NbtCompound apply(@Nullable NbtElement nbtElement) {
    final NbtCompound targetCompound = (nbtElement instanceof final NbtCompound nbtCompound && allowsMerge) ? nbtCompound : new NbtCompound();
    source.forEach((key, nbtFunction) -> {
      if (nbtFunction == null) {
        targetCompound.remove(key);
      } else {
        targetCompound.put(key, nbtFunction.apply(targetCompound.get(key)));
      }
    });
    return targetCompound;
  }
}
