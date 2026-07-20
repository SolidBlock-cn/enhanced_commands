package pers.solid.ecmd.curve;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;
import pers.solid.ecmd.util.DefaultNamespace;

public interface CurveType<T extends Curve> {
  ResourceKey<Registry<CurveType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("curve_type"));
  Registry<CurveType<?>> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);
  Codec<CurveType<?>> CODEC = DefaultNamespace.ENHANCED_COMMANDS.byNameCodecForRegistry(REGISTRY, true);

  MapCodec<T> codec();

  MapCodec<? extends CurveProvider<? extends T>> providerCodec();

  record Simple<T extends Curve>(MapCodec<T> codec, MapCodec<? extends CurveProvider<? extends T>> providerCodec) implements CurveType<T> {}
}
