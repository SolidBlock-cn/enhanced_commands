package pers.solid.ecmd.util;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/**
 * <p>用于处理对象与种子的映射关系的容器类。在一些涉及多个方块函数或方块谓词的情形中，如果各个方块函数应用相同的种子，就有可能出现一些问题，为了确保同一次执行时多个对象的种子互不相同，同时又确保带有同一种子多次执行命令的结果中的种子一致，故创设性类。
 * <p>该类会存储一些对象（依据其内存地址而非散列值）以及对应的种子。使用 {@link #getSeed(Object)} 方法可查询对象的种子，如果该对象没有存储在此容器中，则会分配种子，并将种子保存在该窗口中。对每一个 {@code SeedStorage} 而言，相同对象对应相同的种子，不同对象通常对应不同的种子（不保证）。对于多个 {@code SeedStorage} 而言，如果其初始种子（{@link #initialSeed}）相同，那么只要保持一致的顺序获取各对象的种子，那么结果也是一样的。
 * <p>通常来说，从 {@code SeedStorage} 中获取的第一个对象的种子与 {@link #initialSeed} 保持一致。
 *
 * <p><b>示例：</b></p>
 * <pre>{@code
 *
 *     String a = "string a";
 *     String b = "string b";
 *     String c = "string c";
 *
 *     final SeedStorage<String> seedStorage = new SeedStorage<>(1);
 *     System.out.println(seedStorage.getSeed(a)); // -5451962507482445012
 *     System.out.println(seedStorage.getSeed(b)); // -8136195590166681979
 *     System.out.println(seedStorage.getSeed(c)); // -6810170813813275380
 *
 *     // 生成新的 SeedStorage 对象，并使用和刚刚一致的初始种子。
 *     final SeedStorage<String> seedStorage2 = new SeedStorage<>(1);
 *     System.out.println(seedStorage2.getSeed(a)); // -5451962507482445012
 *     System.out.println(seedStorage2.getSeed(b)); // -8136195590166681979
 *     System.out.println(seedStorage2.getSeed(c)); // -6810170813813275380
 *
 *     // 生成新的 SeedStorage 对象，并使用和刚刚不一致的初始种子。
 *     final SeedStorage<String> seedStorage3 = new SeedStorage<>(2);
 *     System.out.println(seedStorage3.getSeed(a)); // 4233148493373801447
 *     System.out.println(seedStorage3.getSeed(b)); // -5128846648494427149
 *     System.out.println(seedStorage3.getSeed(c)); // -600008877602036303
 *
 *     // 生成新的 SeedStorage 对象，但获取对象的顺序不同。
 *     final SeedStorage<String> seedStorage4 = new SeedStorage<>(2);
 *     System.out.println(seedStorage4.getSeed(a)); // 4233148493373801447
 *     System.out.println(seedStorage4.getSeed(b)); // -5128846648494427149
 *     System.out.println(seedStorage4.getSeed(c)); // -600008877602036303
 * }</pre>
 *
 * @param <K> 对象的类型，可以是 {@code Object}。
 */
public class SeedStorage<K> {
  /**
   * 该容器的初始种子。
   */
  private final long initialSeed;
  private final Reference2LongMap<K> seeds = new Reference2LongOpenHashMap<>();
  private final Long2ObjectMap<PositionalRandomFactory> splitters = new Long2ObjectOpenHashMap<>();

  /**
   * 创建新的种子存储容器。
   *
   * @param initialSeed 初始种子。
   */
  public SeedStorage(long initialSeed) {
    this.initialSeed = initialSeed;
  }

  /**
   * 获取特定对象对应的种子。如果该对象未被存储，则会根据 {@link #initialSeed} 以及该对象的次序分配一个种子。
   *
   * @param key 需要获取种子的对象。
   * @return 该对象对应的种子。
   */
  public long getSeed(K key) {
    return seeds.computeIfAbsent(key, k -> HashCommon.murmurHash3(initialSeed + (seeds.isEmpty() ? 0 : HashCommon.murmurHash3(seeds.size()))));
  }

  /**
   * 获取特定对象对应的种子的 splitter。对于同一对象，不会多次创建 splitter 对象。
   */
  public PositionalRandomFactory getSplitter(K key) {
    return getSplitterForSeed(getSeed(key));
  }

  /**
   * 获取特定种子的 splitter。对于同一种子，不会多次创建 splitter 对象。
   */
  public PositionalRandomFactory getSplitterForSeed(long seed) {
    return splitters.computeIfAbsent(seed, LegacyRandomSource.LegacyPositionalRandomFactory::new);
  }
}
