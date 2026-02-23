package pers.solid.ecmd.function.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

public interface BlockFunctionType<T extends BlockFunction> {
  ResourceKey<Registry<BlockFunctionType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("block_function_type"));
  Registry<BlockFunctionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  @NotNull
  MapCodec<T> getCodec();
}
