package pers.solid.ecmd.util.extension;

import com.google.common.collect.Iterables;
import net.minecraft.network.chat.Component;
import net.minecraft.util.thread.BlockableEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.util.iterator.IteratorTask;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * The interface will be injected into {@link BlockableEventLoop}.
 */
public interface BlockableEventLoopExtension {
  Logger LOGGER = LoggerFactory.getLogger(BlockableEventLoopExtension.class);

  default void addIteratorTask$ec(IteratorTask<?> task) {
    getIteratorTasks$ec().add(task);
    getUUIDToIteratorTasks$ec().put(task.uuid, task);
  }

  default <T> IteratorTask<T> addIteratorTask$ec(Component name, Iterator<T> iterator) {
    final IteratorTask<T> task = new IteratorTask<>(name, UUID.randomUUID(), iterator);
    addIteratorTask$ec(task);
    return task;
  }

  Queue<IteratorTask<?>> getIteratorTasks$ec();

  Map<UUID, IteratorTask<?>> getUUIDToIteratorTasks$ec();

  /**
   * The method is used to handle tasks, such as those created by {@link FillReplaceCommand} when handling quantities of blocks.
   *
   * @see pers.solid.ecmd.command.TasksCommand
   */
  default void ec_advanceTasks() {
    final Queue<IteratorTask<?>> iteratorTasks = getIteratorTasks$ec();
    final Iterator<IteratorTask<?>> limit = Iterables.limit(iteratorTasks, 8).iterator();
    while (limit.hasNext()) {
      final IteratorTask<?> task = limit.next();
      if (task.suspended) continue;
      if (!task.hasNext()) {
        // Remove the task when completed.
        LOGGER.info("Task {} completed.", task);
        limit.remove();
        getUUIDToIteratorTasks$ec().remove(task.uuid);
        continue;
      }
      try {
        task.next();
      } catch (Throwable throwable) {
        LOGGER.error("Error when executing task {}, removing!", task, throwable);
        limit.remove();
        getUUIDToIteratorTasks$ec().remove(task.uuid);
      }
    }
  }
}
