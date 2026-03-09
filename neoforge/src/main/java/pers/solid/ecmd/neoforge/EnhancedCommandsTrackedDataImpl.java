package pers.solid.ecmd.neoforge;

import com.google.common.base.Suppliers;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.EnhancedCommandsTrackedData;
import pers.solid.ecmd.api.InitializeContext;
import pers.solid.ecmd.api.neoforge.InitializeContextImpl;

public class EnhancedCommandsTrackedDataImpl {
  private static final DeferredRegister<EntityDataSerializer<?>> DEFERRED_REGISTER = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, EnhancedCommands.MOD_ID);

  public static void init(InitializeContext context) {
    final InitializeContextImpl impl = (InitializeContextImpl) context;
    DEFERRED_REGISTER.register("region_selection", Suppliers.ofInstance(EnhancedCommandsTrackedData.REGION_SELECTION));

    DEFERRED_REGISTER.register(impl.modEventBus);
  }
}
