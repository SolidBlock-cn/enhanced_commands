package pers.solid.ecmd.neoforge;

import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.ModTrackedData;

public class ModTrackedDataImpl {
  public static final DeferredRegister<EntityDataSerializer<?>> DEFERRED_REGISTER = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, EnhancedCommands.MOD_ID);

  public static void init() {
    DEFERRED_REGISTER.register("region_selection", () -> ModTrackedData.REGION_SELECTION);
  }
}
