package pers.solid.ecmd.extensions;

import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.Iterators;
import net.minecraft.text.Text;
import net.minecraft.util.thread.ThreadExecutor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Queue;

/**
 * The interface will be injected into {@link ThreadExecutor}.
 */
public interface ThreadExecutorExtension {
  Logger LOGGER = LoggerFactory.getLogger(ThreadExecutorExtension.class);

  default void ec_addIteratorTask(IteratorTask<?> task) {
    ec_getIteratorTasks().add(task);
  }

  default void ec_addIteratorTask(Text name, Iterator<?> iterator) {
    ec_addIteratorTask(new IteratorTask<>(name, iterator));
  }

  Queue<IteratorTask<?>> ec_getIteratorTasks();

  default void ec_advanceTasks() {
    final Queue<IteratorTask<?>> iteratorTasks = ec_getIteratorTasks();
    final Iterator<IteratorTask<?>> limit = Iterators.limit(iteratorTasks.iterator(), 8);
    while (limit.hasNext()) {
      final IteratorTask<?> task = limit.next();
      if (!task.hasNext()) {
        // Remove the task when completed.
        LOGGER.info("Task {} completed.", task);
        limit.remove();
        continue;
      }
      try {
        task.next();
      } catch (Throwable throwable) {
        LOGGER.error("Error when executing task {}, removing!",task, throwable);
        limit.remove();
      }
    }
  }

  class IteratorTask<T> extends ForwardingIterator<T> {
    public final Text name;
    private final Iterator<T> delegate;
    private boolean started;

    public IteratorTask(@NotNull Text name, @NotNull Iterator<T> delegate) {
      this.name = name;
      this.delegate = delegate;
    }

    @Override
    public @NotNull Iterator<T> delegate() {
      return delegate;
    }

    @Override
    public T next() {
      if (!started) {
        LOGGER.info("Starting iterator task {}", name.getString());
        started = true;
      }
      return super.next();
    }

    @Override
    public @NotNull String toString() {
      return "IteratorTask[" + name.getString() + ", " + delegate + "]";
    }
  }
}
