package pers.solid.ecmd.util.iterator;

import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.FailableRunnable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.UUID;

public interface IteratorTask extends Iterator<@Nullable FailableRunnable<Throwable>> {

  @Contract(pure = true)
  Component getName();

  @Contract(pure = true)
  UUID getUuid();

  @Contract(pure = true)
  boolean suspended();

  void setSuspended(boolean suspended);

  default void onReceiveCancelCommand() {
  }

  default void onReceiveSuspendCommand() {
  }

  default void onReceiveContinueCommand() {
  }

  default void onError(Throwable throwable) throws Throwable {
    throw throwable;
  }
}
