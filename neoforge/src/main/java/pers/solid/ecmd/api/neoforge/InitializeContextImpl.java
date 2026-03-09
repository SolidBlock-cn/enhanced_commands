package pers.solid.ecmd.api.neoforge;

import net.neoforged.bus.api.IEventBus;
import pers.solid.ecmd.api.InitializeContext;

public class InitializeContextImpl implements InitializeContext {
  public final IEventBus modEventBus;

  public InitializeContextImpl(IEventBus modEventBus) {
    this.modEventBus = modEventBus;
  }
}
