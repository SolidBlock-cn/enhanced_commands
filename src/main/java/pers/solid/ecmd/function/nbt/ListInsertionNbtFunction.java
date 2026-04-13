package pers.solid.ecmd.function.nbt;

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

public record ListInsertionNbtFunction(List<NbtFunction> insertBefore, List<NbtFunction> insertAfter) implements ListNbtFunction {
  public static final MapCodec<ListInsertionNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtFunction.CODEC.listOf().optionalFieldOf("insert_before", ImmutableList.of()).forGetter(ListInsertionNbtFunction::insertBefore),
      NbtFunction.CODEC.listOf().optionalFieldOf("insert_after", ImmutableList.of()).forGetter(ListInsertionNbtFunction::insertAfter)
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
    listTag.addAll(0, IterateUtils.transformFailableArrayList(insertBefore, f -> f.apply(null, context)));
    listTag.addAll(IterateUtils.transformFailableArrayList(insertAfter, f -> f.apply(null, context)));
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
