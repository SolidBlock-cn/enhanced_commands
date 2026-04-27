package pers.solid.ecmd.nbt.function;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 在 NBT 复合标签（参数）中加入另一个 NBT 复合标签（源标签）的内容。相同的键会被覆盖。目标标签已有但是源标签中没有的键不会受到影响。在加入参数中不存在的键时，其值相当于函数对 null 的值。例如：
 * <pre>
 *   {a: b, c: d} 应用于 {a: B, e: F} 结果：{a: b, c: d, e: F}
 *   {a: b, c: {d: e}} 应用于 {c: {f: g}} 结果：{a: b, c: {d: e, f: g}}
 *   {} 应用于 {a: b, c: d} 结果：{a: b, c: d}
 *   {a: b, c: d} 应用于 {} 结果：{a: b, c: d}
 * </pre>
 * 如果参数不是 NBT 复合标签，则直接返回源 NBT 复合标签的值。例如：
 * <pre>
 *   {a: b} 应用于 [1, 2, 3] 结果：{a: b}
 *   {a: b} 应用于 "string" 结果：{a: b}
 * </pre>
 * 在键前加上 {@code -}（横线）和一个空格且不提供值，可以删除一个字段。例如：
 * <pre>
 *   {- a, - c} 应用于 {a: b, c: d} 结果：{}
 * </pre>
 * 如果前面是 {@code =}（等号），则禁用合并功能，会替换整个 NBT 复合标签：
 * <pre>
 *   ={a: b} 应用于 {c: d} 结果：{a: b}
 * </pre>
 * 不允许重复键，重复键会被覆盖。
 *
 * @param allowsMerge 是否允许对 NBT 复合标签进行合并
 */
public record CompoundNbtFunction(Map<String, Optional<NbtFunction>> source, boolean allowsMerge) implements NbtFunction {
  public static final MapCodec<CompoundNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.unboundedMap(Codec.STRING, Codec.optionalField("value", NbtFunction.CODEC, false).codec()).optionalFieldOf("source", ImmutableMap.of()).forGetter(CompoundNbtFunction::source),
      Codec.BOOL.optionalFieldOf("allows_merge", true).forGetter(CompoundNbtFunction::allowsMerge)
  ).apply(i, CompoundNbtFunction::new));

  @Override
  public String expressAsString() {
    return expressAsString(false);
  }

  @Override
  public String expressAsString(boolean requirePrefix) {
    return (allowsMerge ? (requirePrefix ? ": " : "") : "= ") + "{" + source.entrySet().stream().map(entry -> {
      final String key = entry.getKey();
      final Optional<NbtFunction> value = entry.getValue();
      if (value.isEmpty()) {
        return "- " + key;
      } else {
        final String valueAsString = value.get().expressAsString(true);
        return key + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
      }
    }).collect(Collectors.joining(", ")) + "}";
  }

  @Override
  public NbtFunctionType<CompoundNbtFunction> getType() {
    return NbtFunctionTypes.COMPOUND;
  }

  @Override
  public CompoundTag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    final CompoundTag targetCompound = (nbtElement instanceof final CompoundTag nbtCompound && allowsMerge) ? nbtCompound : new CompoundTag();
    for (Map.Entry<String, Optional<NbtFunction>> entry : source.entrySet()) {
      String key = entry.getKey();
      Optional<NbtFunction> nbtFunction = entry.getValue();
      if (nbtFunction.isEmpty()) {
        targetCompound.remove(key);
      } else {
        targetCompound.put(key, nbtFunction.get().apply(targetCompound.get(key), context));
      }
    }
    return targetCompound;
  }
}
