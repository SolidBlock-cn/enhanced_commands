package pers.solid.ecmd.mixins.impl;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.api.EnhancedServerReloadableRegistries;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Mixin(ReloadableServerRegistries.class)
public class ReloadableServerRegistriesMixin {
  /**
   * 在加载可重复加载的过程中，加载本模组注册的注册表。注意这些注册表不会通过常规的非可重复加载的动态注册表流程加载。
   *
   * @see pers.solid.ecmd.mixins.general.RegistryDataLoaderMixin#excludeModRegistryData(Stream)
   */
  @ModifyReceiver(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"))
  private static Stream<CompletableFuture<WritableRegistry<?>>> appendMoreCompletableFutures(Stream<CompletableFuture<WritableRegistry<?>>> instance, @Local RegistryOps<JsonElement> ops, @Local(argsOnly = true) ResourceManager resourceManager, @Local(argsOnly = true) Executor prepareExecutor) {
    return Stream.concat(instance, EnhancedServerReloadableRegistries.getEnhancedMutableRegistries(ops, resourceManager, prepareExecutor));
  }
}
