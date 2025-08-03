package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

public interface EntityPredicateType<T extends EntityPredicate> {
  RegistryKey<Registry<EntityPredicateType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("entity_predicate_entry_type"));
  Registry<EntityPredicateType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

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
