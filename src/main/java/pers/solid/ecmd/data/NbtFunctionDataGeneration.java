package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.nbt.function.NbtFunction;

public class NbtFunctionDataGeneration implements DynamicRegistryGenerationBridge<NbtFunction> {
  private static ResourceKey<NbtFunction> of(String value) {
    return ResourceKey.create(NbtFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "NBT Functions (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<NbtFunction> context) {
  }
}
