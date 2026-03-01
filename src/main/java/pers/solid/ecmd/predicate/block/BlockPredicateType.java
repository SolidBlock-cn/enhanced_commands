package pers.solid.ecmd.predicate.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

public interface BlockPredicateType<T extends BlockPredicate> {
  ResourceKey<Registry<BlockPredicateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("block_predicate_type"));
  Registry<BlockPredicateType<?>> REGISTRY = RegistryBridge.buildAndRegisterSimple(REGISTRY_KEY);

  @NotNull
  MapCodec<T> getCodec();
}
