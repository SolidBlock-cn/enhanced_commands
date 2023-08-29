package pers.solid.ecmd.mixin;

import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(ThreadExecutor.class)
public class ThreadExecutorMixin implements ThreadExecutorExtension {
  @Unique
  private final Queue<IteratorTask<?>> iteratorTasks = new ConcurrentLinkedQueue<>();

  @Override
  public void ec_addIteratorTask(IteratorTask<?> task) {
    iteratorTasks.add(task);
  }

  @Override
  public Queue<IteratorTask<?>> ec_getIteratorTasks() {
    return iteratorTasks;
  }

  @Inject(method = "runTasks()V", at = @At("TAIL"))
  public void injectedRunTasks(CallbackInfo ci) {
    ec_advanceTasks();
  }
}
