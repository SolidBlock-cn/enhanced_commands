package pers.solid.ecmd.nbt.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>此 NBT 函数用于在列表内插入元素。其语法为：
 * <pre>{@code
 *   [前面的多个<项>（用逗号隔开）, ..., 后面的多个<项>（用逗号隔开）]
 * } </pre>
 * 其中，每个{@code <项>}的语法为：
 * <pre>{@code
 *   <NBT 函数> | <索引位置>（整数） : <NBT 函数>
 * }</pre>
 * <p>对于没有指定索引位置的项，省略号前面的项会插入在列表的前面，省略号后面的项会插入在列表的后面。对于指定了索引位置的项，会依次插入在索引位置，操作次序取决于出现的顺序，在省略号前或后均无影响。
 * <p>此 NBT 函数的操作顺序为：先在列表前面插入未指定索引的元素，再在后面插入未指定索引的元素，再插入指定了索引的元素。
 * <p>示例：
 * <ul>
 *   <li>{@code [a, b, ..., c, d]}：在列表最前面插入两个元素 {@code "a"} 和 {@code "b"}，再在列表后面插入两个元素 {@code "c"} 和 {@code "d"}。
 *   <li>{@code [..., 123, 456]}：在列表后面插入两个元素：{@code 123}、{@code 456}。
 *   <li>{@code [0: a, ..., 2: b]}：将 {@code "a"} 插入在第 1 个元素位置，再将 {@code "b"} 插入在第 3 个元素位置。
 *   <li>{@code [0: a, b, ..., 3: c, d]}：将 {@code "b"} 插入在最前面，再将 {@code "d"} 插入在最后面，再将 {@code "a"} 插入在第 1 个元素位置（即最前面），再将 {@code "c"} 插入在第 4 个元素位置。
 * </ul>
 *
 * @param insertBefore     需要插入在列表前面的元素。
 * @param insertAfter      需要插入在列表后面的元素。
 * @param insertPositional 需要插入在指定位置的元素。
 */
public record ListInsertionNbtFunction(List<NbtFunction> insertBefore, List<NbtFunction> insertAfter, List<PositionalListEntry<NbtFunction>> insertPositional) implements ListNbtFunction, RequiresValidation {
  public static final MapCodec<ListInsertionNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtFunction.CODEC.listOf().optionalFieldOf("insert_before", ImmutableList.of()).forGetter(ListInsertionNbtFunction::insertBefore),
      NbtFunction.CODEC.listOf().optionalFieldOf("insert_after", ImmutableList.of()).forGetter(ListInsertionNbtFunction::insertAfter),
      PositionalListEntry.codec(NbtFunction.CODEC).listOf().optionalFieldOf("insert_positional", ImmutableList.of()).forGetter(ListInsertionNbtFunction::insertPositional)
  ).apply(i, ListInsertionNbtFunction::new));

  @Override
  public String expressAsString() {
    return Streams.concat(
        insertBefore.stream().map(NbtFunction::expressAsString),
        Stream.of("..."),
        insertAfter.stream().map(NbtFunction::expressAsString)
    ).collect(Collectors.joining(", ", "[", "]"));
  }

  @Override
  public NbtFunctionType<ListInsertionNbtFunction> getType() {
    return NbtFunctionTypes.LIST_INSERTION;
  }

  @Override
  public ListTag applyOnList(ListTag listTag, ExecutionContext context) throws CommandSyntaxException {
    try {
      listTag.addAll(0, IterateUtils.transformFailableArrayList(insertBefore, f -> f.apply(null, context)));
    } catch (UnsupportedOperationException | IndexOutOfBoundsException ignore) {
    }
    try {
      listTag.addAll(IterateUtils.transformFailableArrayList(insertAfter, f -> f.apply(null, context)));
    } catch (UnsupportedOperationException | IndexOutOfBoundsException ignore) {
    }
    for (PositionalListEntry<NbtFunction> entry : insertPositional) {
      try {
        listTag.add(entry.index(), entry.value().apply(null, context));
      } catch (UnsupportedOperationException | IndexOutOfBoundsException ignore) {
      }
    }
    return listTag;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return Iterables.concat(insertBefore, insertAfter, Lists.transform(insertPositional, PositionalListEntry::value));
  }
}
