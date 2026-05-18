package pers.solid.ecmd.util.iterator;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.UUID;

public class ForwardingIteratorTask extends AbstractIteratorTask {
  final Iterator<@Nullable Runnable> forward;

  public ForwardingIteratorTask(Component name, UUID uuid, Iterator<@Nullable Runnable> forward, CommandSourceStack source) {
    super(name, uuid, source);
    this.forward = forward;
  }

  @Override
  public boolean hasNext() {
    return forward.hasNext();
  }

  @Override
  public @Nullable Runnable next() {
    return forward.next();
  }
}
