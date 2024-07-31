package pers.solid.ecmd.function.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

public interface BlockFunctionType<T extends BlockFunction> {
  RegistryKey<Registry<BlockFunctionType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("block_function_type"));
  Registry<BlockFunctionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  @NotNull
  MapCodec<T> getCodec();
}
