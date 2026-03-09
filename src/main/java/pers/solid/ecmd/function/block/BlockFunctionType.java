package pers.solid.ecmd.function.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

public interface BlockFunctionType<T extends BlockFunction> {
  ResourceKey<Registry<BlockFunctionType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("block_function_type"));
  Registry<BlockFunctionType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);

  @NotNull
  MapCodec<T> getCodec();
}
