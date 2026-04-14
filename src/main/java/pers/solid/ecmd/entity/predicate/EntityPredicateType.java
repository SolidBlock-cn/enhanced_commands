package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

public interface EntityPredicateType<T extends EntityPredicate> {
  ResourceKey<Registry<EntityPredicateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("entity_predicate_entry_type"));
  Registry<EntityPredicateType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);

  @NotNull
  MapCodec<T> codec();

  static <T extends EntityPredicate> EntityPredicateType<T> create(@NotNull MapCodec<T> codec) {
    return new Simple<>(codec);
  }

  record Simple<T extends EntityPredicate>(@NotNull MapCodec<T> codec) implements EntityPredicateType<T> {
    @Override
    public @NotNull MapCodec<T> codec() {
      return codec;
    }
  }
}
