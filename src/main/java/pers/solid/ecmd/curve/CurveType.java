package pers.solid.ecmd.curve;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

public interface CurveType<T extends Curve> {
  ResourceKey<Registry<CurveType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("curve_type"));
  Registry<CurveType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);

  @NotNull MapCodec<T> getCodec();

  @NotNull
  MapCodec<? extends CurveProvider<? extends T>> getArgumentCodec();
}
