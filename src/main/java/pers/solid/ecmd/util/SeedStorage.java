package pers.solid.ecmd.util;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.RandomSplitter;

import java.util.IdentityHashMap;

public class SeedStorage {
  private final long initialSeed;
  private final Reference2LongMap<Object> seeds = new Reference2LongOpenHashMap<>();
  private final IdentityHashMap<Object, RandomSplitter> splitters = new IdentityHashMap<>();

  public SeedStorage(long initialSeed) {
    this.initialSeed = initialSeed;
  }

  public long getSeed(Object key) {
    return seeds.computeIfAbsent(key, k -> HashCommon.murmurHash3(initialSeed + (splitters.isEmpty() ? 0 : HashCommon.murmurHash3(splitters.size()))));
  }

  public RandomSplitter getSplitter(Object key) {
    return splitters.computeIfAbsent(key, k -> new CheckedRandom.Splitter(getSeed(k)));
  }
}
