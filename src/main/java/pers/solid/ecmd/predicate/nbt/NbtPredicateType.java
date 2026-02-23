package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;

public interface NbtPredicateType<T extends NbtPredicate> {

  ResourceKey<Registry<NbtPredicateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("nbt_predicate_type"));
  Registry<NbtPredicateType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  MapCodec<T> getCodec();
}
