package pers.solid.ecmd.util.codec;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.ListCodec;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * 集的 codec。类似于 {@link ListCodec}，编码时会按照列表编码，但是解码时直接读取为列表，含有重复的内容会忽略。。
 *
 * @param elementCodec 集合元素的 codec。
 * @param <A>          集的元素的类型。
 */
public record SetCodec<A>(Codec<A> elementCodec) implements Codec<Set<A>> {
  @Override
  public <T> DataResult<Pair<Set<A>, T>> decode(DynamicOps<T> ops, T input) {
    return ops.getList(input).setLifecycle(Lifecycle.stable()).flatMap(resultConsumer -> {
      final ImmutableSet.Builder<A> read = ImmutableSet.builder();
      final Stream.Builder<T> failed = Stream.builder();
      final AtomicReference<DataResult<Unit>> result = new AtomicReference<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));

      resultConsumer.accept(inputElement -> {
        final DataResult<Pair<A, T>> element = elementCodec.decode(ops, inputElement);
        element.error().ifPresent(e -> failed.add(inputElement));
        result.set(result.get().apply2stable((r, v) -> {
          // check if duplicate elements?
          read.add(v.getFirst());
          return r;
        }, element));
      });

      final ImmutableSet<A> elements = read.build();
      final T errors = ops.createList(failed.build());

      final Pair<Set<A>, T> pair = Pair.of(elements, errors);

      return result.get().map(unit -> pair).setPartial(pair);
    });
  }

  @Override
  public <T> DataResult<T> encode(Set<A> input, DynamicOps<T> ops, T prefix) {
    final ListBuilder<T> builder = ops.listBuilder();

    for (final A a : input) {
      builder.add(elementCodec.encodeStart(ops, a));
    }

    return builder.build(prefix);
  }
}
