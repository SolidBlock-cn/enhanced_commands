package pers.solid.ecmd.mixins.general;

import com.google.common.collect.MapMaker;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.util.extension.BlockableEventLoopExtension;
import pers.solid.ecmd.util.iterator.IteratorTask;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(BlockableEventLoop.class)
public class BlockableEventLoopMixin implements BlockableEventLoopExtension {
  @Unique
  private final Map<UUID, IteratorTask> enhanced_commands$uuidToTask = new MapMaker().weakValues().makeMap();
  @Unique
  private final Queue<IteratorTask> enhanced_commands$iteratorTasks = new ConcurrentLinkedQueue<>();

  @Override
  public void addIteratorTask$ec(IteratorTask task) {
    enhanced_commands$iteratorTasks.add(task);
    enhanced_commands$uuidToTask.put(task.getUuid(), task);
  }

  @Override
  public Queue<IteratorTask> getIteratorTasks$ec() {
    return enhanced_commands$iteratorTasks;
  }

  @Override
  public Map<UUID, IteratorTask> getUUIDToIteratorTasks$ec() {
    return enhanced_commands$uuidToTask;
  }
}
