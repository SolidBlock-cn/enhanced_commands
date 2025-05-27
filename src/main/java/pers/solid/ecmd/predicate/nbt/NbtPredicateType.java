package pers.solid.ecmd.predicate.nbt;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import pers.solid.ecmd.EnhancedCommands;

public interface NbtPredicateType<T extends NbtPredicate> {

  RegistryKey<Registry<NbtPredicateType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("nbt_predicate_type"));
  Registry<NbtPredicateType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  MapCodec<T> getCodec();
}
