package pers.solid.ecmd.predicate.property;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public enum Comparator implements StringRepresentable {
  EQ("=", Object::equals),
  GT(">", (actual, expected) -> actual.compareTo(expected) > 0),
  GE(">=", (actual, expected) -> actual.compareTo(expected) >= 0),
  LT("<", (actual, expected) -> actual.compareTo(expected) < 0),
  LE("<=", (actual, expected) -> actual.compareTo(expected) <= 0),
  NE("!=", (actual, expected) -> !actual.equals(expected));

  public static final Map<String, Comparator> NAME_TO_VALUE = Util.make(new ImmutableMap.Builder<String, Comparator>(), builder -> Arrays.stream(values()).forEach(comparator -> builder.put(comparator.getSerializedName(), comparator))).put("=!", NE).build();
  public static final StringIdentifiableCodec<Comparator> CODEC = StringIdentifiableCodec.create(Comparator.values());
  public static final MapCodec<Comparator> FIELD_CODEC = CODEC.optionalFieldOf("comparator").xmap(comparator -> comparator.orElse(EQ), Optional::of);

  private final String name;
  private final BiPredicate biPredicate;

  Comparator(String name, BiPredicate biPredicate) {
    this.name = name;
    this.biPredicate = biPredicate;
  }

  public static Comparator fromName(String name) {
    return NAME_TO_VALUE.get(name);
  }

  @Override
  public @NotNull String getSerializedName() {
    return name;
  }

  public <T extends Comparable<T>> boolean test(T actual, T expected) {
    return biPredicate.test(actual, expected);
  }

  public <T extends Comparable<T>> boolean test(BlockState actual, Property<T> property, T expected) {
    return biPredicate.test(actual.getValue(property), expected);
  }

  public <T extends Comparable<T>> boolean parseAndTest(BlockState actual, Property<T> property, String name) {
    final Optional<T> parse = property.getValue(name);
    if (this == NE && parse.isEmpty()) {
      return true;
    }
    return parse.filter(t -> biPredicate.test(actual.getValue(property), t)).isPresent();
  }

  public boolean compareDouble(double actual, double expected) {
    return switch (this) {
      case EQ -> actual == expected;
      case GT -> actual > expected;
      case GE -> actual >= expected;
      case LT -> actual < expected;
      case LE -> actual <= expected;
      case NE -> actual != expected;
    };
  }

  public boolean compareFloat(float actual, float expected) {
    return switch (this) {
      case EQ -> actual == expected;
      case GT -> actual > expected;
      case GE -> actual >= expected;
      case LT -> actual < expected;
      case LE -> actual <= expected;
      case NE -> actual != expected;
    };
  }

  public boolean compareLong(long actual, long expected) {
    return switch (this) {
      case EQ -> actual == expected;
      case GT -> actual > expected;
      case GE -> actual >= expected;
      case LT -> actual < expected;
      case LE -> actual <= expected;
      case NE -> actual != expected;
    };
  }

  public boolean compareInt(int actual, int expected) {
    return switch (this) {
      case EQ -> actual == expected;
      case GT -> actual > expected;
      case GE -> actual >= expected;
      case LT -> actual < expected;
      case LE -> actual <= expected;
      case NE -> actual != expected;
    };
  }

  public boolean compareShort(short actual, short expected) {
    return switch (this) {
      case EQ -> actual == expected;
      case GT -> actual > expected;
      case GE -> actual >= expected;
      case LT -> actual < expected;
      case LE -> actual <= expected;
      case NE -> actual != expected;
    };
  }

  public boolean compareByte(byte actual, byte expected) {
    return switch (this) {
      case EQ -> actual == expected;
      case GT -> actual > expected;
      case GE -> actual >= expected;
      case LT -> actual < expected;
      case LE -> actual <= expected;
      case NE -> actual != expected;
    };
  }

  public interface BiPredicate {
    boolean testObject(Comparable<Object> actual, Object expected);

    @SuppressWarnings("unchecked")
    default <T extends Comparable<T>> boolean test(T actual, T expected) {
      return testObject((Comparable<Object>) (Comparable<?>) actual, expected);
    }
  }
}
