package pers.solid.ecmd.block.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.util.DefaultNamespace;

public interface BlockFunctionType<T extends BlockFunction> {
  ResourceKey<Registry<BlockFunctionType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("block_function_type"));
  Registry<BlockFunctionType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);
  Codec<BlockFunctionType<?>> CODEC = DefaultNamespace.ENHANCED_COMMANDS.byNameCodecForRegistry(BlockFunctionType.REGISTRY, true);

  MapCodec<T> codec();

  record Simple<T extends BlockFunction>(MapCodec<T> codec) implements BlockFunctionType<T> {}
}
