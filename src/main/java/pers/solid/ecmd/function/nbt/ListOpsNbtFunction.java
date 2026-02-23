package pers.solid.ecmd.function.nbt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 这是专用于处理列表的 NBT 函数。如果目标 NBT 元素不是列表，将返回对空列表应用函数的值。默认情况下，会替换整个列表的内容，例如：
 * <pre>
 *   [a, b, c]([d, e, f]) = [a, b, c]
 * </pre>
 * 可以指定特定位置的值。如果特定的位置不存在，则不执行操作：
 * <pre>
 *   [2: b]([d, e, f]) = [d, e, b]
 *   [0: a, 2: b]([d, e, f]) = [a, e, b]
 *   [0: a, 3: b]([d, e, f]) = [a, e, f]
 * </pre>
 * 可以前插值和后插值：
 * <pre>
 *   [A, ...]({d, e, f}) = [A, d, e, f]
 *   [..., B, C]({d, e, f}) = [d, e, f, B, C]
 * </pre>
 * 插值也可以指定插值的位置：
 * <pre>
 *   [1: A, ...]({d, e, f}) = [d, A, e, f]
 *   [..., 1: A]({d, e, f}) = [d, e, A, f]
 * </pre>
 * 指定插值的位置时，也可以一次插入多个值：
 * <pre>
 *   [1: A, B, C, ...]({d, e, f}) = [d, A, B, C, e, f]
 * </pre>
 * 可以先修改值再插值：
 * <pre>
 *   [a, b, c; A, ..., B]({d, e, f}) = [A, a, b, c, B]
 *   [2: a; A, ..., B]({d, e, f}) = [A, d, a, f, B]
 * </pre>
 */
public record ListOpsNbtFunction(@NotNull List<NbtFunction> valueReplacements, @NotNull Map<Integer, NbtFunction> positionalFunctions, @NotNull Map<Integer, List<NbtFunction>> positionalInsertions) implements NbtFunction {
  public static final MapCodec<ListOpsNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.list(NbtFunction.CODEC).optionalFieldOf("value_replacements", ImmutableList.of()).forGetter(ListOpsNbtFunction::valueReplacements),
      Codec.unboundedMap(Codec.INT, NbtFunction.CODEC).optionalFieldOf("positional_functions", ImmutableMap.of()).forGetter(ListOpsNbtFunction::positionalFunctions),
      Codec.unboundedMap(Codec.INT, Codec.list(NbtFunction.CODEC)).optionalFieldOf("positional_insertions", ImmutableMap.of()).forGetter(ListOpsNbtFunction::positionalInsertions)
  ).apply(i, ListOpsNbtFunction::new));

  @Override
  public @NotNull String asString() {
    return asString(false);
  }

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    final Function<Map.Entry<Integer, NbtFunction>, String> indexValueToStringMapper = entry -> {
      final int index = entry.getKey();
      final String valueAsString = entry.getValue().asString();
      return index + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
    };
    final Function<Map.Entry<Integer, List<NbtFunction>>, String> indexValuesToStringMapper = entry -> {
      final int index = entry.getKey();
      MutableBoolean elementRequiresPrefix = new MutableBoolean(true);
      final String valueAsString = entry.getValue().stream().map(nbtFunction -> {
        final boolean value = elementRequiresPrefix.booleanValue();
        elementRequiresPrefix.setFalse();
        return nbtFunction.asString(value);
      }).collect(Collectors.joining(", "));
      return index + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
    };
    return (requirePrefix ? ": " : "") + "[" + Stream.<String>concat(
        valueReplacements.isEmpty() ? Stream.empty() : valueReplacements.stream().map(NbtFunction::asString),
        positionalFunctions.isEmpty() ? Stream.empty() : positionalFunctions.entrySet().stream().map(indexValueToStringMapper)
    ).collect(Collectors.joining(", ")) + ((!valueReplacements.isEmpty() || !positionalFunctions.isEmpty()) && !positionalInsertions.isEmpty() ? "; " : "") + (positionalInsertions.isEmpty() ? "" : Streams.concat(
        positionalInsertions.entrySet().stream().filter(entry -> entry.getKey() < 0).map(indexValuesToStringMapper),
        Stream.of("..."),
        Streams.concat(
            positionalInsertions.entrySet().stream().filter(entry -> entry.getKey() >= 0).map(indexValuesToStringMapper)
        )).collect(Collectors.joining(", "))) + "]";
  }

  @Override
  public @NotNull NbtFunctionType<?> getType() {
    return Type.LIST_OPS_TYPE;
  }

  @Override
  public @NotNull Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    final ListTag targetList = nbtElement instanceof final ListTag nbtList ? nbtList : new ListTag();
    if (!valueReplacements.isEmpty()) {
      targetList.clear();
      try {
        for (NbtFunction nbtFunction : valueReplacements) {
          targetList.add(nbtFunction.apply(null, context));
        }
      } catch (UnsupportedOperationException ignored) {
      }
    }
    if (!positionalFunctions.isEmpty()) {
      for (Map.Entry<Integer, NbtFunction> entry : positionalFunctions.entrySet()) {
        int index = entry.getKey();
        final NbtFunction function = entry.getValue();
        if (index < 0) {
          index += targetList.size();
        }
        try {
          targetList.setTag(index, function.apply(targetList.get(index), context));
        } catch (UnsupportedOperationException | IndexOutOfBoundsException ignored) {
        }
      }
    }
    if (!positionalInsertions.isEmpty()) {
      final int[] positiveIndexes = positionalInsertions.keySet().stream().mapToInt(Integer::intValue).filter(value -> value >= 0).sorted().toArray();
      final IntStream negativeIndexes = positionalInsertions.keySet().stream().mapToInt(Integer::intValue).filter(value -> value < 0).sorted();

      final int[] array = IntStream.concat(IntStream.range(0, positiveIndexes.length).map(i -> positiveIndexes[positiveIndexes.length - 1 - i]), negativeIndexes).toArray();
      for (int i = 0, arrayLength = array.length; i < arrayLength; i++) {
        int index = array[i];
        final List<NbtFunction> function = positionalInsertions.get(index);
        if (index < 0) {
          index += targetList.size() + 1;
        }
        try {
          for (NbtFunction nbtFunction : function) {
            targetList.add(index + i, nbtFunction.apply(null, context));
          }
        } catch (IndexOutOfBoundsException | UnsupportedOperationException ignored) {
        }
      }
    }
    return targetList;
  }

  public enum Type implements NbtFunctionType<ListOpsNbtFunction> {
    LIST_OPS_TYPE;

    @Override
    public MapCodec<ListOpsNbtFunction> getCodec() {
      return CODEC;
    }
  }
}
