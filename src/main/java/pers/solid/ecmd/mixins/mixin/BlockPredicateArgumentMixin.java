package pers.solid.ecmd.mixins.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.util.mixin.ArgumentTypeExtension;
import pers.solid.ecmd.util.mixin.ForwardingBlockPredicateArgument;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Mixin(BlockPredicateArgument.class)
public abstract class BlockPredicateArgumentMixin implements ArgumentTypeExtension {
  @Unique
  private pers.solid.ecmd.argument.BlockPredicateArgumentType modArgumentType;
  @Unique
  private boolean extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(CommandBuildContext commandBuildContext, CallbackInfo ci) {
    this.modArgumentType = new pers.solid.ecmd.argument.BlockPredicateArgumentType(commandBuildContext);
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/blocks/BlockPredicateArgument$Result;", at = @At("HEAD"), cancellable = true)
  private void injectedParse(StringReader stringReader, CallbackInfoReturnable<BlockPredicateArgument.Result> cir) throws CommandSyntaxException {
    if (modArgumentType != null && extension) {
      cir.setReturnValue(new ForwardingBlockPredicateArgument(modArgumentType.parse(stringReader)));
    }
  }

  @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
  private <S> void injectedListSuggestions(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    if (modArgumentType != null && extension)
      cir.setReturnValue(modArgumentType.listSuggestions(context, builder));
  }

  @Inject(method = "getBlockPredicate", at = @At("RETURN"))
  private static void injectedGetBlockPredicate(CommandContext<CommandSourceStack> context, String name, CallbackInfoReturnable<Predicate<BlockInWorld>> cir) throws CommandSyntaxException {
    final Predicate<BlockInWorld> returnValue = cir.getReturnValue();
    if (returnValue instanceof ForwardingBlockPredicateArgument forwardedBlockStateArgument) {
      forwardedBlockStateArgument.setSource(context.getSource());
    }
  }

  @Override
  public boolean enhanced_hasExtension() {
    return extension;
  }

  @Override
  public void enhanced_setExtension(boolean extension) {
    this.extension = extension;
  }
}
