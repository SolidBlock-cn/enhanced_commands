package pers.solid.ecmd.function.block;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

public class BlockFunctionContext extends ExecutionContext {
  public final int flags;
  public final int modFlags;

  public BlockFunctionContext(int flags, int modFlags, RandomSource random, CommandSourceStack source, @Nullable Long seed) {
    super(random, source, seed);
    this.flags = flags;
    this.modFlags = modFlags;
  }
}
