package pers.solid.ecmd.mixins.general;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
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
  private @Nullable pers.solid.ecmd.argument.BlockPredicateArgument enhanced_commands$modArgumentType;
  @Unique
  private boolean enhanced_commands$extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(CommandBuildContext context, CallbackInfo ci) {
    this.enhanced_commands$modArgumentType = new pers.solid.ecmd.argument.BlockPredicateArgument(context);
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/blocks/BlockPredicateArgument$Result;", at = @At("HEAD"), cancellable = true)
  private void injectedParse(StringReader reader, CallbackInfoReturnable<BlockPredicateArgument.Result> cir) throws CommandSyntaxException {
    if (enhanced_commands$modArgumentType != null && enhanced_commands$extension) {
      cir.setReturnValue(new ForwardingBlockPredicateArgument(enhanced_commands$modArgumentType.parse(reader)));
    }
  }

  @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
  private <S> void injectedListSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    if (enhanced_commands$modArgumentType != null && enhanced_commands$extension)
      cir.setReturnValue(enhanced_commands$modArgumentType.listSuggestions(commandContext, suggestionsBuilder));
  }

  @Inject(method = "getBlockPredicate", at = @At("RETURN"))
  private static void injectedGetBlockPredicate(CommandContext<CommandSourceStack> context, String name, CallbackInfoReturnable<Predicate<BlockInWorld>> cir) {
    final Predicate<BlockInWorld> returnValue = cir.getReturnValue();
    if (returnValue instanceof ForwardingBlockPredicateArgument forwardedBlockStateArgument) {
      forwardedBlockStateArgument.setSource(context.getSource());
    }
  }

  @Override
  public boolean enhanced_hasExtension() {
    return enhanced_commands$extension;
  }

  @Override
  public void enhanced_setExtension(boolean extension) {
    this.enhanced_commands$extension = extension;
  }
}
