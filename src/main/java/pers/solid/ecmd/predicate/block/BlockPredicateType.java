package pers.solid.ecmd.predicate.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

public interface BlockPredicateType<T extends BlockPredicate> {
  RegistryKey<Registry<BlockPredicateType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("block_predicate_type"));
  Registry<BlockPredicateType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  @NotNull
  MapCodec<T> getCodec();
}
