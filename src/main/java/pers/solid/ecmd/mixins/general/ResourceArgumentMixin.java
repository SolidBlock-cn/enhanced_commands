package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(ResourceArgument.class)
public abstract class ResourceArgumentMixin<T> {

  @Shadow
  @Final
  ResourceKey<? extends Registry<T>> registryKey;

  @Shadow
  @Final
  private HolderLookup<T> registryLookup;

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/core/Holder$Reference;", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;"))
  public void injectedParse(StringReader builder, CallbackInfoReturnable<Holder.Reference<T>> cir, @Share("cursorBeforeId") LocalIntRef localIntRef) {
    localIntRef.set(builder.getCursor());
  }

  @Inject(method = "listSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/HolderLookup;listElementIds()Ljava/util/stream/Stream;"), cancellable = true)
  public <S> void suggestWithTooltip(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    MixinShared.mixinSuggestWithTooltip(registryKey, registryLookup, suggestionsBuilder, cir);
  }

  @ModifyArg(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/core/Holder$Reference;", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"))
  public Supplier<CommandSyntaxException> modifiedParseThrow(Supplier<CommandSyntaxException> original, @Share("cursorBeforeId") LocalIntRef localIntRef, @Local(argsOnly = true) StringReader builder, @Local ResourceLocation resourceLocation) {
    return MixinShared.mixinModifiedParseThrow(registryKey, original, localIntRef, builder, resourceLocation);
  }
}
