package pers.solid.ecmd.mixins.mixin;

import com.google.common.collect.MapMaker;
import net.minecraft.util.thread.ThreadExecutor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(ThreadExecutor.class)
public class ThreadExecutorMixin implements ThreadExecutorExtension {
  @Unique
  private final Map<UUID, IteratorTask<?>> uuidToTask = new MapMaker().weakValues().makeMap();
  @Unique
  private final Queue<IteratorTask<?>> iteratorTasks = new ConcurrentLinkedQueue<>();

  @Override
  public void ec_addIteratorTask(IteratorTask<?> task) {
    iteratorTasks.add(task);
    uuidToTask.put(task.uuid, task);
  }

  @Override
  public @NotNull Queue<IteratorTask<?>> ec_getIteratorTasks() {
    return iteratorTasks;
  }

  @Override
  public @NotNull Map<UUID, IteratorTask<?>> ec_getUUIDToIteratorTasks() {
    return uuidToTask;
  }
}
