package pers.solid.ecmd.fabric;

import net.minecraft.network.syncher.EntityDataSerializers;
import pers.solid.ecmd.ModTrackedData;

public class ModTrackedDataImpl {
  public static void init() {
    EntityDataSerializers.registerSerializer(ModTrackedData.REGION_SELECTION);
  }
}
