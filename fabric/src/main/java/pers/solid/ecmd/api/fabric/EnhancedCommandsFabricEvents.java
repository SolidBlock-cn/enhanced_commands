package pers.solid.ecmd.api.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import pers.solid.ecmd.api.FlipStateCallback;

public final class EnhancedCommandsFabricEvents {
  public static final Event<FlipStateCallback> FLIP_STATE = EventFactory.createArrayBacked(FlipStateCallback.class, flipStateEvents -> (intermediate, original) -> {
    for (FlipStateCallback flipStateCallback : flipStateEvents) {
      intermediate = flipStateCallback.getFlippedState(intermediate, original);
    }
    return intermediate;
  });
}
