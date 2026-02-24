package pers.solid.ecmd.mixins.mixin;

import com.google.common.collect.MapMaker;
import net.minecraft.util.thread.BlockableEventLoop;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.mixins.ext.BlockableEventLoopExtension;
import pers.solid.ecmd.util.iterator.IteratorTask;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(BlockableEventLoop.class)
public class BlockableEventLoopMixin implements BlockableEventLoopExtension {
  @Unique
  private final Map<UUID, IteratorTask<?>> uuidToTask = new MapMaker().weakValues().makeMap();
  @Unique
  private final Queue<IteratorTask<?>> iteratorTasks = new ConcurrentLinkedQueue<>();

  @Override
  public void addIteratorTask$ec(IteratorTask<?> task) {
    iteratorTasks.add(task);
    uuidToTask.put(task.uuid, task);
  }

  @Override
  public @NotNull Queue<IteratorTask<?>> getIteratorTasks$ec() {
    return iteratorTasks;
  }

  @Override
  public @NotNull Map<UUID, IteratorTask<?>> getUUIDToIteratorTasks$ec() {
    return uuidToTask;
  }
}
