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
import pers.solid.ecmd.registry.EnhancedReloadableRegistries;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Mixin(ReloadableServerRegistries.class)
public class ReloadableServerRegistriesMixin {
  @ModifyReceiver(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"))
  private static Stream<CompletableFuture<WritableRegistry<?>>> appendMoreCompletableFutures(Stream<CompletableFuture<WritableRegistry<?>>> instance, @Local RegistryOps<JsonElement> ops, @Local(argsOnly = true) ResourceManager resourceManager, @Local(argsOnly = true) Executor prepareExecutor) {
    return Stream.concat(instance, EnhancedReloadableRegistries.getEnhancedMutableRegistries(ops, resourceManager, prepareExecutor));
  }
}
