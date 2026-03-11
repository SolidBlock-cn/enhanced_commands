package pers.solid.ecmd.mixins.general;

import com.google.common.base.Functions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pers.solid.ecmd.config.GeneralParsingConfig;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(SharedSuggestionProvider.class)
public interface SharedSuggestionProviderMixin {

  @ModifyArg(method = "suggestResource(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;filterResources(Ljava/lang/Iterable;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"), index = 3)
  private static Consumer<ResourceLocation> suggestIdentifiersEmitNamespace(Consumer<ResourceLocation> action, @Local String remaining, @Local(argsOnly = true) SuggestionsBuilder builder) {
    return MixinShared.getModifiedConsumer(Functions.identity(), action, remaining, (identifier, identifier2) -> builder.suggest(identifier.getPath()));
  }

  @ModifyArg(method = "suggestResource(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;filterResources(Ljava/lang/Iterable;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"), index = 4)
  private static Consumer<ResourceLocation> suggestIdentifiersEmitNamespaceWithPrefix(Consumer<ResourceLocation> action, @Local(ordinal = 1) String remaining, @Local(argsOnly = true) SuggestionsBuilder builder, @Local(argsOnly = true) String prefix) {
    return MixinShared.getModifiedConsumer(Functions.identity(), action, remaining, (identifier, identifier2) -> builder.suggest(prefix + identifier.getPath()));
  }

  @ModifyArg(method = "suggestResource(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;filterResources(Ljava/lang/Iterable;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"), index = 3)
  private static <T> Consumer<T> suggestFromIdentifiers(Consumer<T> action, @Local String remaining, @Local(argsOnly = true) SuggestionsBuilder builder, @Local(argsOnly = true, ordinal = 0) Function<T, ResourceLocation> identifier, @Local(argsOnly = true, ordinal = 1) Function<T, Message> tooltip) {
    return MixinShared.getModifiedConsumer(identifier, action, remaining, (identifier1, t) -> builder.suggest(identifier1.getPath(), tooltip.apply(t)));
  }

  /**
   * 在提供 id 的建议时，即使 id 不是原版默认命令空间的，也允许提供相应建议。例如，输入了 {@code path} 时，可建议 {@code minecraft:path} 和 {@code non_minecraft_namespace:path}。
   * <br>
   * 在 NeoForge 中可能不起作用。
   */
  @ModifyExpressionValue(method = "filterResources(Ljava/lang/Iterable;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"), require = 0)
  private static boolean suggestNonDefaultNamespacedIds(boolean original) {
    return original || GeneralParsingConfig.current.suggestNonDefaultNamespacedIds;
  }
}
