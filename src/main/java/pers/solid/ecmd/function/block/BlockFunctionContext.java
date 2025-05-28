package pers.solid.ecmd.function.block;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.block.ExecutionContext;

public class BlockFunctionContext extends ExecutionContext {
  public final int flags;
  public final int modFlags;

  public BlockFunctionContext(int flags, int modFlags, Random random, ServerCommandSource source, @Nullable Long seed) {
    super(random, source, seed);
    this.flags = flags;
    this.modFlags = modFlags;
  }
}
