package pers.solid.ecmd.nbt.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.ListTag;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
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
public record ListOpsNbtFunction(List<NbtFunction> valueReplacements, List<PositionalListEntry<NbtFunction>> positionalFunctions) implements ListNbtFunction {
  public static final MapCodec<ListOpsNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.list(NbtFunction.CODEC).optionalFieldOf("value_replacements", ImmutableList.of()).forGetter(ListOpsNbtFunction::valueReplacements),
      PositionalListEntry.codec(NbtFunction.CODEC).listOf().optionalFieldOf("positional_functions", ImmutableList.of()).forGetter(ListOpsNbtFunction::positionalFunctions)
  ).apply(i, ListOpsNbtFunction::new));

  @Override
  public String asString() {
    return asString(false);
  }

  @Override
  public String asString(boolean requirePrefix) {
    final Function<PositionalListEntry<NbtFunction>, String> indexValueToStringMapper = entry -> {
      final int index = entry.index();
      final String valueAsString = entry.value().asString();
      return index + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
    };
    return (requirePrefix ? ": " : "") + "[" + Stream.<String>concat(
        valueReplacements.isEmpty() ? Stream.empty() : valueReplacements.stream().map(NbtFunction::asString),
        positionalFunctions.isEmpty() ? Stream.empty() : positionalFunctions.stream().map(indexValueToStringMapper)
    ).collect(Collectors.joining(", ")) + "]";
  }

  @Override
  public Type getType() {
    return Type.LIST_OPS_TYPE;
  }

  @Override
  public ListTag applyOnList(ListTag listTag, ExecutionContext context) throws CommandSyntaxException {
    if (!valueReplacements.isEmpty()) {
      listTag.clear();
      try {
        for (NbtFunction nbtFunction : valueReplacements) {
          listTag.add(nbtFunction.apply(null, context));
        }
      } catch (UnsupportedOperationException ignored) {
      }
    }
    if (!positionalFunctions.isEmpty()) {
      for (PositionalListEntry<NbtFunction> entry : positionalFunctions) {
        int index = entry.index();
        final NbtFunction function = entry.value();
        if (index < 0) {
          index += listTag.size();
        }
        try {
          listTag.setTag(index, function.apply(listTag.get(index), context));
        } catch (UnsupportedOperationException | IndexOutOfBoundsException ignored) {
        }
      }
    }
    return listTag;
  }

  public enum Type implements NbtFunctionType<ListOpsNbtFunction> {
    LIST_OPS_TYPE;

    @Override
    public MapCodec<ListOpsNbtFunction> getCodec() {
      return CODEC;
    }
  }
}
