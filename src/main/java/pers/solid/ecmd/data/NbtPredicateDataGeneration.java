package pers.solid.ecmd.data;

import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;

public class NbtPredicateDataGeneration implements DynamicRegistryGenerationBridge<NbtPredicate> {
  private static ResourceKey<NbtPredicate> of(String value) {
    return ResourceKey.create(NbtPredicate.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "NBT Predicates (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<NbtPredicate> context) {
  }
}
