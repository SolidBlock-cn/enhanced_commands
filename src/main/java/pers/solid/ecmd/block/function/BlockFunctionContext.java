package pers.solid.ecmd.block.function;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.PositionProvider;

public class BlockFunctionContext extends ExecutionContext {
  public final int flags;
  public final int modFlags;

  public BlockFunctionContext(int flags, int modFlags, RandomSource random, PositionProvider source, @Nullable Long seed) {
    super(random, source, seed);
    this.flags = flags;
    this.modFlags = modFlags;
  }
}
