package pers.solid.ecmd.block;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import pers.solid.ecmd.util.iterator.CatchingIterator;

import java.util.Iterator;

/**
 * 在执行方块操作时遇到没有加载的区块而进行的中断操作。
 */
@NullMarked
public final class UnloadedPosException extends RuntimeException {
  public final BlockPos unloadedPos;

  public UnloadedPosException(BlockPos unloadedPos) {
    this.unloadedPos = unloadedPos;
  }

  public static <T> Iterable<T> catching(Iterable<T> t) {
    return () -> new CatchingIterator<>(t.iterator(), e -> {
      if (!(e instanceof UnloadedPosException)) {
        throw e;
      }
    });
  }

  public static <T extends @Nullable Object> CatchingIterator<T> catching(Iterator<T> t) {
    return new CatchingIterator<>(t, e -> {
      if (!(e instanceof UnloadedPosException)) {
        throw e;
      }
    });
  }
}
