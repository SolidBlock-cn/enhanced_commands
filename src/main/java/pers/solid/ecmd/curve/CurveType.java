package pers.solid.ecmd.curve;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

public interface CurveType<T extends Curve> {
  ResourceKey<Registry<CurveType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("curve_type"));
  Registry<CurveType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  @NotNull MapCodec<T> getCodec();

  @NotNull
  MapCodec<? extends CurveProvider<? extends T>> getArgumentCodec();
}
