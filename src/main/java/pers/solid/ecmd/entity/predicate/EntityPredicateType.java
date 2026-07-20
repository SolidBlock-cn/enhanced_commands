package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.util.DefaultNamespace;

public interface EntityPredicateType<T extends EntityPredicate> {
  ResourceKey<Registry<EntityPredicateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("entity_predicate_entry_type"));
  Registry<EntityPredicateType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);
  Codec<EntityPredicateType<?>> CODEC = DefaultNamespace.ENHANCED_COMMANDS.byNameCodecForRegistry(REGISTRY, true);

  MapCodec<T> codec();

  static <T extends EntityPredicate> EntityPredicateType<T> create(MapCodec<T> codec) {
    return new Simple<>(codec);
  }

  record Simple<T extends EntityPredicate>(MapCodec<T> codec) implements EntityPredicateType<T> {
    @Override
    public MapCodec<T> codec() {
      return codec;
    }
  }
}
