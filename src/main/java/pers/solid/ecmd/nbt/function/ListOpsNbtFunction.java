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
 * <p>这是专用于处理列表的 NBT 函数。如果目标 NBT 元素不是列表，将返回对空列表应用函数的值。
 * <p>如果列表定义了未指定特定位置的值，那么这些元素会先替换整个列表的内容，例如：
 * <pre>
 *   [a, b, c] 应用于 [d, e, f] 效果：[a, b, c]
 * </pre>
 * <p>可以设置特定位置的值。如果特定的位置不存在，则不执行操作：
 * <pre>
 *   [2: b] 应用于 [d, e, f] 效果：[d, e, b]
 *   [0: a, 2: b] 应用于 [d, e, f] 效果：[a, e, b]
 *   [0: a, 3: b] 应用于[d, e, f] 效果：[a, e, f]
 * </pre>
 * <p>如果没有指定任何替换元素和特定位置的元素：会将任何列表转换为空列表：
 * <pre>
 *   [] 应用于 [a, b, c] 效果：[]
 * </pre>
 *
 * <p>此 NBT 函数仅可设置指定位置的值。如需要在列表中插入元素，请使用 {@link ListInsertionNbtFunction}。在 NBT 函数语法中，只要中括号中有省略号，就会被视为 {@link ListInsertionNbtFunction}。
 *
 * @param valueReplacements   需要被替换的元素列表。当此列表不是空，或者当 {@link #positionalFunctions} 为空时，会先清空原有列表，并直接替换为此列表的内容。
 * @param positionalFunctions 需要插入的元素及其对应索引的列表。
 */
public record ListOpsNbtFunction(List<NbtFunction> valueReplacements, List<PositionalListEntry<NbtFunction>> positionalFunctions) implements ListNbtFunction {
  public static final MapCodec<ListOpsNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.list(NbtFunction.CODEC).optionalFieldOf("value_replacements", ImmutableList.of()).forGetter(ListOpsNbtFunction::valueReplacements),
      PositionalListEntry.codec(NbtFunction.CODEC).listOf().optionalFieldOf("positional_functions", ImmutableList.of()).forGetter(ListOpsNbtFunction::positionalFunctions)
  ).apply(i, ListOpsNbtFunction::new));

  @Override
  public String expressAsString() {
    return asString(false);
  }

  @Override
  public String asString(boolean requirePrefix) {
    final Function<PositionalListEntry<NbtFunction>, String> indexValueToStringMapper = entry -> {
      final int index = entry.index();
      final String valueAsString = entry.value().expressAsString();
      return index + (valueAsString.startsWith(":") ? "" : " ") + valueAsString;
    };
    return (requirePrefix ? ": " : "") + "[" + Stream.<String>concat(
        valueReplacements.isEmpty() ? Stream.empty() : valueReplacements.stream().map(NbtFunction::expressAsString),
        positionalFunctions.isEmpty() ? Stream.empty() : positionalFunctions.stream().map(indexValueToStringMapper)
    ).collect(Collectors.joining(", ")) + "]";
  }

  @Override
  public NbtFunctionType<ListOpsNbtFunction> getType() {
    return NbtFunctionTypes.LIST_OPS;
  }

  @Override
  public ListTag applyOnList(ListTag listTag, ExecutionContext context) throws CommandSyntaxException {
    if (!valueReplacements.isEmpty() || positionalFunctions.isEmpty()) {
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
}
