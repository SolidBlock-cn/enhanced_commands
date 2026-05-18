package pers.solid.ecmd.util.iterator;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import pers.solid.ecmd.exception.CommandRuntimeException;

import java.util.UUID;

public abstract class AbstractIteratorTask implements IteratorTask {
  protected final Component name;
  protected final UUID uuid;
  protected final CommandSourceStack source;
  protected boolean suspended = false;

  protected AbstractIteratorTask(Component name, UUID uuid, CommandSourceStack source) {
    this.name = name;
    this.uuid = uuid;
    this.source = source;
  }

  @Override
  public Component getName() {
    return name;
  }

  @Override
  public UUID getUuid() {
    return uuid;
  }

  @Override
  public boolean suspended() {
    return suspended;
  }

  @Override
  public void setSuspended(boolean suspended) {
    this.suspended = suspended;
  }

  @Override
  public void onError(Throwable throwable) throws Throwable {
    if (throwable instanceof CommandSyntaxException e) {
      source.sendFailure(ComponentUtils.fromMessage(e.getRawMessage()));
    } else if (throwable instanceof CommandRuntimeException e) {
      source.sendFailure(ComponentUtils.fromMessage(e.rawMessage));
    } else if (throwable.getCause() instanceof CommandSyntaxException e) {
      source.sendFailure(ComponentUtils.fromMessage(e.getRawMessage()));
    } else {
      throw throwable;
    }
  }
}
