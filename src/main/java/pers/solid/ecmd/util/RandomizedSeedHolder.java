package pers.solid.ecmd.util;

import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableLong;

import java.util.OptionalLong;

public sealed interface RandomizedSeedHolder {
  Random RANDOM = Random.create();

  long seed();

  boolean canRandomize();

  RandomizedSeedHolder getRefreshed(Random random);

  OptionalLong toFixedSeed();

  static RandomizedSeedHolder random() {
    return new Randomized(RANDOM.nextLong());
  }

  static RandomizedSeedHolder of(long value) {
    return new Fixed(value);
  }

  static RandomizedSeedHolder ofOptional(OptionalLong optionalLong) {
    return optionalLong.isPresent() ? of(optionalLong.getAsLong()) : random();
  }

  final class Randomized extends MutableLong implements RandomizedSeedHolder {
    public Randomized(long value) {
      super(value);
    }

    @Override
    public long seed() {
      return getValue();
    }

    @Override
    public boolean canRandomize() {
      return true;
    }

    @Override
    public RandomizedSeedHolder getRefreshed(Random random) {
      return of(random.nextLong());
    }

    @Override
    public OptionalLong toFixedSeed() {
      return OptionalLong.empty();
    }
  }

  record Fixed(long seed) implements RandomizedSeedHolder {
    @Override
    public boolean canRandomize() {
      return false;
    }

    @Override
    public RandomizedSeedHolder getRefreshed(Random random) {
      return this;
    }

    @Override
    public OptionalLong toFixedSeed() {
      return OptionalLong.of(seed);
    }
  }
}
