package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.util.mixin.EnhancedRedirectModifier;

import java.util.Map;

@Mixin(value = CommandDispatcher.class, remap = false)
public abstract class CommandDispatcherMixin {
  @SuppressWarnings("unchecked")
  @ModifyArg(method = "execute(Lcom/mojang/brigadier/ParseResults;)I", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/RedirectModifier;apply(Lcom/mojang/brigadier/context/CommandContext;)Ljava/util/Collection;")))
  public <S> Object modifyArgumentsForContext(Object e, @Local RedirectModifier<S> modifier, @Local S source, @Local(ordinal = 1) CommandContext<S> previousContext) throws CommandSyntaxException {
    final CommandContext<S> context = (CommandContext<S>) e;
    if (modifier instanceof EnhancedRedirectModifier.Multiple<S> enhancedRedirectModifier) {
      final Map<String, ParsedArgument<S, ?>> arguments = ((CommandContextAccessor<S>) context).getArguments();
      enhancedRedirectModifier.modifyArguments(arguments, ((CommandContextAccessor<S>) previousContext).getArguments(), source);
    }
    return context;
  }
}
