package pers.solid.ecmd.predicate.property;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public enum Comparator implements StringIdentifiable {
  EQ("=", Object::equals),
  GT(">", (actual, expected) -> actual.compareTo(expected) > 0),
  GE(">=", (actual, expected) -> actual.compareTo(expected) >= 0),
  LT("<", (actual, expected) -> actual.compareTo(expected) < 0),
  LE("<=", (actual, expected) -> actual.compareTo(expected) <= 0),
  NE("!=", (actual, expected) -> !actual.equals(expected));

  public static final Map<String, Comparator> NAME_TO_VALUE = Arrays.stream(values()).collect(ImmutableMap.toImmutableMap(Comparator::asString, Functions.identity()));

  private final String name;
  private final BiPredicate biPredicate;

  Comparator(String name, BiPredicate biPredicate) {
    this.name = name;
    this.biPredicate = biPredicate;
  }

  @Override
  public String asString() {
    return name;
  }

  public static Comparator fromName(String name) {
    return NAME_TO_VALUE.get(name);
  }

  public <T extends Comparable<T>> boolean test(T actual, T expected) {
    return biPredicate.test(actual, expected);
  }

  public <T extends Comparable<T>> boolean test(BlockState actual, Property<T> property, T expected) {
    return biPredicate.test(actual.get(property), expected);
  }

  public <T extends Comparable<T>> boolean parseAndTest(BlockState actual, Property<T> property, String name) {
    final Optional<T> parse = property.parse(name);
    return parse.filter(t -> biPredicate.test(actual.get(property), t)).isPresent();
  }

  public interface BiPredicate {
    boolean testObject(Comparable<Object> actual, Object expected);

    @SuppressWarnings("unchecked")
    default <T extends Comparable<T>> boolean test(T actual, T expected) {
      return testObject((Comparable<Object>) (Comparable<?>) actual, expected);
    }
  }
}
