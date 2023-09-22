package pers.solid.ecmd.mixin;

import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(ThreadExecutor.class)
public class ThreadExecutorMixin implements ThreadExecutorExtension {
  @Unique
  private final Queue<IteratorTask<?>> iteratorTasks = new ConcurrentLinkedQueue<>();
  @Unique
  private final Map<UUID, WeakReference<IteratorTask<?>>> uuidToTask = new HashMap<>();

  @Override
  public void ec_addIteratorTask(IteratorTask<?> task) {
    iteratorTasks.add(task);
    uuidToTask.put(task.uuid, new WeakReference<>(task));
  }

  @Override
  public Queue<IteratorTask<?>> ec_getIteratorTasks() {
    return iteratorTasks;
  }

  @Override
  public Map<UUID, WeakReference<IteratorTask<?>>> ec_getUUIDToIteratorTasks() {
    return uuidToTask;
  }
}
