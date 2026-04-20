package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.util.DefaultNamespace;

public interface ItemPredicateType<T extends ItemPredicate> {
  ResourceKey<Registry<ItemPredicateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("item_predicate_type"));
  Registry<ItemPredicateType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);
  Codec<ItemPredicateType<?>> CODEC = DefaultNamespace.ENHANCED_COMMANDS.byNameCodecForRegistry(REGISTRY);

  MapCodec<T> codec();

  record Simple<T extends ItemPredicate>(MapCodec<T> codec) implements ItemPredicateType<T> {
  }
}
