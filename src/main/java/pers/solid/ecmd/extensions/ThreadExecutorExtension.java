package pers.solid.ecmd.extensions;

import com.google.common.collect.Iterables;
import net.minecraft.text.Text;
import net.minecraft.util.thread.ThreadExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * The interface will be injected into {@link ThreadExecutor}.
 */
public interface ThreadExecutorExtension {
  Logger LOGGER = LoggerFactory.getLogger(ThreadExecutorExtension.class);

  default void ec_addIteratorTask(IteratorTask<?> task) {
    ec_getIteratorTasks().add(task);
    ec_getUUIDToIteratorTasks().put(task.uuid, new WeakReference<>(task));
  }

  default void ec_addIteratorTask(Text name, Iterator<?> iterator) {
    ec_addIteratorTask(new IteratorTask<>(name, UUID.randomUUID(), iterator));
  }

  Queue<IteratorTask<?>> ec_getIteratorTasks();

  Map<UUID, WeakReference<IteratorTask<?>>> ec_getUUIDToIteratorTasks();

  /**
   * The method is used to handle tasks, such as those created by {@link pers.solid.ecmd.command.SetBlocksCommand} when handling quantities of blocks.
   *
   * @see pers.solid.ecmd.command.TasksCommand
   */
  default void ec_advanceTasks() {
    final Queue<IteratorTask<?>> iteratorTasks = ec_getIteratorTasks();
    final Iterator<IteratorTask<?>> limit = Iterables.limit(iteratorTasks, 8).iterator();
    while (limit.hasNext()) {
      final IteratorTask<?> task = limit.next();
      if (task.suspended) continue;
      if (!task.hasNext()) {
        // Remove the task when completed.
        LOGGER.info("Task {} completed.", task);
        limit.remove();
        ec_getUUIDToIteratorTasks().remove(task.uuid);
        continue;
      }
      try {
        task.next();
      } catch (Throwable throwable) {
        LOGGER.error("Error when executing task {}, removing!", task, throwable);
        limit.remove();
        ec_getUUIDToIteratorTasks().remove(task.uuid);
      }
    }
  }

}
