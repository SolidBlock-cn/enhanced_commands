package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.mixins.accessor.CommandContextAccessor;
import pers.solid.ecmd.util.EnhancedRedirectModifier;

import java.util.Map;

@Mixin(ContextChain.class)
public abstract class ContextChainMixin {
  @SuppressWarnings("unchecked")
  @ModifyArg(method = "runModifier", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/RedirectModifier;apply(Lcom/mojang/brigadier/context/CommandContext;)Ljava/util/Collection;"), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/RedirectModifier;apply(Lcom/mojang/brigadier/context/CommandContext;)Ljava/util/Collection;")))
  private static <S> CommandContext<S> modifyArgumentsForContext(CommandContext<S> context, @Local RedirectModifier<S> modifier, @Local(argsOnly = true) S source, @Local(ordinal = 1) CommandContext<S> previousContext) throws CommandSyntaxException {
    if (modifier instanceof EnhancedRedirectModifier.Multiple<S> enhancedRedirectModifier) {
      final Map<String, ParsedArgument<S, ?>> arguments = ((CommandContextAccessor<S>) context).getArguments();
      enhancedRedirectModifier.modifyArguments(arguments, ((CommandContextAccessor<S>) previousContext).getArguments(), source);
    }
    return context;
  }
}
