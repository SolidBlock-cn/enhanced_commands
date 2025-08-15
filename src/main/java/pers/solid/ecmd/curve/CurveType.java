package pers.solid.ecmd.curve;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;

public interface CurveType<T extends Curve> {
  RegistryKey<Registry<CurveType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("curve_type"));
  Registry<CurveType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  @NotNull MapCodec<T> getCodec();

  @NotNull
  MapCodec<? extends CurveArgument<? extends T>> getArgumentCodec();
}
