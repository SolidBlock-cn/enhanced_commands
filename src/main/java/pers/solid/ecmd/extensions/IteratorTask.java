package pers.solid.ecmd.extensions;

import com.google.common.collect.ForwardingIterator;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.UUID;

public class IteratorTask<T> extends ForwardingIterator<T> {
  public final Text name;
  public final UUID uuid;
  private final Iterator<T> delegate;
  private boolean started;
  public boolean suspended = false;

  public IteratorTask(@NotNull Text name, @NotNull UUID uuid, @NotNull Iterator<T> delegate) {
    this.name = name;
    this.uuid = uuid;
    this.delegate = delegate;
  }

  @Override
  public @NotNull Iterator<T> delegate() {
    return delegate;
  }

  @Override
  public T next() {
    if (!started) {
      ThreadExecutorExtension.LOGGER.info("Starting iterator task {}", name.getString());
      started = true;
    }
    return super.next();
  }

  @Override
  public @NotNull String toString() {
    return "IteratorTask[" + name.getString() + ", " + delegate + "]";
  }
}
