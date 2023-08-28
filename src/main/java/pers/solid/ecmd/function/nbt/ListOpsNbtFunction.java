package pers.solid.ecmd.function.nbt;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
public record ListOpsNbtFunction(List<NbtFunction> valueReplacements, Int2ObjectMap<NbtFunction> positionalFunctions, Int2ObjectMap<List<NbtFunction>> positionalInsertions) implements NbtFunction {
  @Override
  public @NotNull String asString(boolean requirePrefix) {
    final Function<Int2ObjectMap.Entry<NbtFunction>, String> indexValueToStringMapper = entry -> {
      final int index = entry.getIntKey();
      final String valueAsString = entry.getValue().asString(true);
      return index + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
    };
    final Function<Int2ObjectMap.Entry<List<NbtFunction>>, String> indexValuesToStringMapper = entry -> {
      final int index = entry.getIntKey();
      MutableBoolean elementRequiresPrefix = new MutableBoolean(true);
      final String valueAsString = entry.getValue().stream().map(nbtFunction -> {
        final boolean value = elementRequiresPrefix.booleanValue();
        elementRequiresPrefix.setFalse();
        return nbtFunction.asString(value);
      }).collect(Collectors.joining(", "));
      return index + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
    };
    return (requirePrefix ? ": " : "") + "[" + Stream.<String>concat(
        valueReplacements == null ? Stream.empty() : valueReplacements.stream().map(NbtFunction::asString),
        positionalFunctions == null ? Stream.empty() : positionalFunctions.int2ObjectEntrySet().stream().map(indexValueToStringMapper)
    ).collect(Collectors.joining(", ")) + ((valueReplacements != null || positionalFunctions != null) && positionalInsertions != null ? "; " : "") + (positionalInsertions == null ? "" : Streams.concat(
        positionalInsertions.int2ObjectEntrySet().stream().filter(entry -> entry.getIntKey() < 0).map(indexValuesToStringMapper),
        Stream.of("..."),
        Streams.concat(
            positionalInsertions.int2ObjectEntrySet().stream().filter(entry -> entry.getIntKey() >= 0).map(indexValuesToStringMapper)
        )).collect(Collectors.joining(", "))) + "]";
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement) {
    final NbtList targetList = nbtElement instanceof final NbtList nbtList ? nbtList : new NbtList();
    if (valueReplacements != null) {
      targetList.clear();
      try {
        targetList.addAll(Lists.transform(valueReplacements, nbtFunction -> nbtFunction.apply(null)));
      } catch (UnsupportedOperationException ignored) {}
    }
    if (positionalFunctions != null) {
      for (Int2ObjectMap.Entry<NbtFunction> entry : positionalFunctions.int2ObjectEntrySet()) {
        final int index = entry.getIntKey();
        final NbtFunction function = entry.getValue();
        if (index < targetList.size()) {
          try {
            targetList.setElement(index, function.apply(targetList.get(index)));
          } catch (UnsupportedOperationException ignored) {}
        }
      }
    }
    if (positionalInsertions != null) {
      final int[] positiveIndexes = positionalInsertions.keySet().intStream().filter(value -> value >= 0).sorted().toArray();
      final IntStream negativeIndexes = positionalInsertions.keySet().intStream().filter(value -> value < 0).sorted();
      IntStream.concat(IntStream.range(0, positiveIndexes.length).map(i -> positiveIndexes[positiveIndexes.length - 1 - i]), negativeIndexes).forEach(index -> {
        final List<NbtFunction> function = positionalInsertions.get(index);
        if (index < 0) {
          index += targetList.size() + 1;
        }
        try {
          targetList.addAll(index, Lists.transform(function, f -> f.apply(null)));
        } catch (IndexOutOfBoundsException | UnsupportedOperationException ignored) {}
      });
    }
    return targetList;
  }
}
