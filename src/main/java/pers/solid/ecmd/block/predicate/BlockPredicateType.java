package pers.solid.ecmd.block.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

public interface BlockPredicateType<T extends BlockPredicate> {
  ResourceKey<Registry<BlockPredicateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("block_predicate_type"));
  Registry<BlockPredicateType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);

  MapCodec<T> codec();

  record Simple<T extends BlockPredicate>(MapCodec<T> codec) implements BlockPredicateType<T> {}
}
