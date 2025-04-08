package pers.solid.ecmd.function.block;

import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.block.BlockPredicateContext;

public class BlockFunctionContext extends BlockPredicateContext {
  public final int flags;
  public final int modFlags;

  public BlockFunctionContext(int flags, int modFlags, Random random, @Nullable Long seed) {
    super(random, seed);
    this.flags = flags;
    this.modFlags = modFlags;
  }
}
