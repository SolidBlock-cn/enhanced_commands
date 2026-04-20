package pers.solid.ecmd.nbt.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.util.DefaultNamespace;

public interface NbtPredicateType<T extends NbtPredicate> {

  ResourceKey<Registry<NbtPredicateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("nbt_predicate_type"));
  Registry<NbtPredicateType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);
  Codec<NbtPredicateType<?>> CODEC = DefaultNamespace.ENHANCED_COMMANDS.byNameCodecForRegistry(REGISTRY);

  MapCodec<T> codec();

  record Simple<T extends NbtPredicate>(MapCodec<T> codec) implements NbtPredicateType<T> {}
}
