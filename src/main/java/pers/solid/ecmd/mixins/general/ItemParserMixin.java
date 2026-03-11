package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.config.ItemParsingConfig;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.Map;
import java.util.function.Consumer;

@Mixin(ItemParser.class)
public abstract class ItemParserMixin {
  @Mixin(targets = "net.minecraft.commands.arguments.item.ItemParser$State")
  public abstract static class StateMixin {
    @WrapOperation(method = "suggestComponentAssignmentOrRemoval", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;suggest(Ljava/lang/String;)Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;"), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/item/ItemParser$State;suggestComponent(Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;")), remap = false)
    private SuggestionsBuilder suggestRemovedComponentConditionally(SuggestionsBuilder instance, String text, Operation<SuggestionsBuilder> original) {
      if (instance.getRemaining().isEmpty() || !ItemParsingConfig.current.fixComponentRemovalSuggestion) {
        return original.call(instance, text);
      }
      return instance;
    }

    @ModifyArg(method = "suggestComponent(Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;filterResources(Ljava/lang/Iterable;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"), index = 3)
    private Consumer<Map.Entry<ResourceKey<DataComponentType<?>>, DataComponentType<?>>> simplifyComponentTypeId(Consumer<Map.Entry<ResourceKey<DataComponentType<?>>, DataComponentType<?>>> resourceConsumer, @Local(argsOnly = true) SuggestionsBuilder builder, @Local(argsOnly = true) String suffix) {
      final String remaining = builder.getRemaining();
      return MixinShared.getModifiedConsumer(
          entry -> entry.getKey().location(),
          resourceConsumer,
          remaining,
          (identifier, entry) -> builder.suggest(identifier.getPath() + suffix)

      );
    }
  }
}
