package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.registry.EnhancedServerReloadableRegistries;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderMixin {
  /**
   * 在加载原版的动态注册表（不可加载）时，排除本模组注册的可重复加载的注册表，因为这些表会在专门的事件中加载，不应重复。
   *
   * @see pers.solid.ecmd.mixins.impl.ReloadableServerRegistriesMixin#appendMoreCompletableFutures(Stream, RegistryOps, ResourceManager, Executor)
   */
  @ModifyExpressionValue(method = "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryDataLoader;createContext(Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/resources/RegistryOps$RegistryInfoLookup;")))
  private static Stream<RegistryDataLoader.RegistryData<?>> excludeModRegistryData(Stream<RegistryDataLoader.RegistryData<?>> original) {
    final Set<ResourceKey<? extends Registry<?>>> resourceKeys = EnhancedServerReloadableRegistries.getRegistry().keySet();
    return original.filter(registryData -> !resourceKeys.contains(registryData.key()));
  }
}
