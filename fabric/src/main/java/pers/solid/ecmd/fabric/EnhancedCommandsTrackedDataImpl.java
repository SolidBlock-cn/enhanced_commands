package pers.solid.ecmd.fabric;

import net.minecraft.network.syncher.EntityDataSerializers;
import pers.solid.ecmd.EnhancedCommandsTrackedData;
import pers.solid.ecmd.InitializeContext;

public class EnhancedCommandsTrackedDataImpl {
  public static void init(InitializeContext context) {
    EntityDataSerializers.registerSerializer(EnhancedCommandsTrackedData.REGION_SELECTION);
  }
}
