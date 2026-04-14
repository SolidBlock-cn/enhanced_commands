package pers.solid.ecmd.nbt.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.ListTag;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ListInsertionNbtFunction(List<NbtFunction> insertBefore, List<NbtFunction> insertAfter, List<PositionalListEntry<NbtFunction>> insertPositional) implements ListNbtFunction {
  public static final MapCodec<ListInsertionNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtFunction.CODEC.listOf().optionalFieldOf("insert_before", ImmutableList.of()).forGetter(ListInsertionNbtFunction::insertBefore),
      NbtFunction.CODEC.listOf().optionalFieldOf("insert_after", ImmutableList.of()).forGetter(ListInsertionNbtFunction::insertAfter),
      PositionalListEntry.codec(NbtFunction.CODEC).listOf().optionalFieldOf("insert_positional", ImmutableList.of()).forGetter(ListInsertionNbtFunction::insertPositional)
  ).apply(i, ListInsertionNbtFunction::new));

  @Override
  public String asString() {
    return Streams.concat(
        insertBefore.stream().map(NbtFunction::asString),
        Stream.of("..."),
        insertAfter.stream().map(NbtFunction::asString)
    ).collect(Collectors.joining(", ", "[", "]"));
  }

  @Override
  public Type getType() {
    return Type.LIST_INSERTION_TYPE;
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

  public enum Type implements NbtFunctionType<ListInsertionNbtFunction> {
    LIST_INSERTION_TYPE;

    @Override
    public MapCodec<ListInsertionNbtFunction> getCodec() {
      return CODEC;
    }
  }
}
