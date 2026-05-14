package pers.solid.ecmd.util.iterator;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.FailableRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.UUID;

public class ForwardingIteratorTask extends AbstractIteratorTask {
  final Iterator<@Nullable FailableRunnable<Throwable>> forward;

  public ForwardingIteratorTask(Component name, UUID uuid, Iterator<@Nullable FailableRunnable<Throwable>> forward, CommandSourceStack source) {
    super(name, uuid, source);
    this.forward = forward;
  }

  @Override
  public boolean hasNext() {
    return forward.hasNext();
  }

  @Override
  public @Nullable FailableRunnable<Throwable> next() {
    return forward.next();
  }
}
