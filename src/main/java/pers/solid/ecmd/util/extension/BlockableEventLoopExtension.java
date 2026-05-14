package pers.solid.ecmd.util.extension;

import com.google.common.collect.Iterables;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import org.apache.commons.lang3.function.FailableRunnable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.iterator.ForwardingIteratorTask;
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

  default void addIteratorTask$ec(IteratorTask task) {
    getIteratorTasks$ec().add(task);
    getUUIDToIteratorTasks$ec().put(task.getUuid(), task);
  }

  default IteratorTask addIteratorTask$ec(Component name, Iterator<@Nullable FailableRunnable<Throwable>> iterator, CommandSourceStack source) {
    final IteratorTask task = new ForwardingIteratorTask(name, UUID.randomUUID(), iterator, source);
    addIteratorTask$ec(task);
    return task;
  }

  Queue<IteratorTask> getIteratorTasks$ec();

  Map<UUID, IteratorTask> getUUIDToIteratorTasks$ec();

  default void receiveCommandRuntimeException(CommandRuntimeException commandRuntimeException) {
    // todo 错误处理应当改由各 task 自行进行
    final Component message = ComponentUtils.fromMessage(commandRuntimeException.rawMessage);
    if (this instanceof MinecraftServer server) {
      server.createCommandSourceStack().sendFailure(message);
    } else if (this instanceof Minecraft client) {
      client.gui.getChat().addMessage(message);
      client.getNarrator().sayNow(message);
    } else {
      LOGGER.warn("Does not know how to feedback CommandRuntimeException: {}", message.getString());
    }
  }

  /**
   * The method is used to handle tasks, such as those created by {@link FillReplaceCommand} when handling quantities of blocks.
   *
   * @see pers.solid.ecmd.command.TasksCommand
   */
  default void ec_advanceTasks() {
    final Queue<IteratorTask> iteratorTasks = getIteratorTasks$ec();
    final Iterator<IteratorTask> limit = Iterables.limit(iteratorTasks, 8).iterator();
    while (limit.hasNext()) {
      final IteratorTask task = limit.next();
      if (task.suspended()) continue;
      if (!task.hasNext()) {
        // Remove the task when completed.
        LOGGER.info("Task {} completed.", task.getName());
        limit.remove();
        getUUIDToIteratorTasks$ec().remove(task.getUuid());
        continue;
      }
      try {
        final FailableRunnable<Throwable> next = task.next();
        if (next != null) {
          next.run();
        }
      } catch (Throwable throwable) {
        try {
          task.onError(throwable);
        } catch (Throwable stillThrown) {
          LOGGER.error("Error when executing task {}, removing!", task, throwable);
        }
        limit.remove();
        getUUIDToIteratorTasks$ec().remove(task.getUuid());
      }
    }
  }
}
