package pers.solid.ecmd.predicate.block;

import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.SeedStorage;

import java.util.OptionalLong;

public class BlockPredicateContext {
  public final Random random;
  private @Nullable Long seed;
  private @Nullable SeedStorage<Object> splitterStorage;

  public BlockPredicateContext(Random random, @Nullable Long seed) {
    this.random = random;
    this.seed = seed;
  }

  public long getSeed() {
    if (seed == null) {
      return seed = random.nextLong();
    } else {
      return seed;
    }
  }

  protected @NotNull SeedStorage<Object> getSplitterStorage() {
    if (splitterStorage == null) {
      return splitterStorage = new SeedStorage<>(getSeed());
    } else {
      return splitterStorage;
    }
  }

  public long getSeed(Object key) {
    return getSplitterStorage().getSeed(key);
  }

  public @NotNull RandomSplitter getSplitter(Object key) {
    return getSplitterStorage().getSplitter(key);
  }

  public @NotNull RandomSplitter getSplitterForSeed(long seed) {
    return getSplitterStorage().getSplitterForSeed(seed);
  }

  public @NotNull RandomSplitter getSplitterForOptionalSeed(Object key, OptionalLong seed) {
    return seed.isPresent() ? getSplitterForSeed(seed.getAsLong()) : getSplitter(key);
  }
}
