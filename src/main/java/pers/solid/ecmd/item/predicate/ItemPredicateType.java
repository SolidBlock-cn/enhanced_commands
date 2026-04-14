package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

public interface ItemPredicateType<T extends ItemPredicate> {
  ResourceKey<Registry<ItemPredicateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("item_predicate_type"));
  Registry<ItemPredicateType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);

  @NotNull
  MapCodec<T> getCodec();
}
