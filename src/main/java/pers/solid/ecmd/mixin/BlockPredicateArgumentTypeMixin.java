package pers.solid.ecmd.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
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

@Mixin(BlockPredicateArgumentType.class)
public abstract class BlockPredicateArgumentTypeMixin implements ArgumentTypeExtension {
  @Unique
  private pers.solid.ecmd.argument.BlockPredicateArgumentType modArgumentType;
  @Unique
  private boolean extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
    this.modArgumentType = new pers.solid.ecmd.argument.BlockPredicateArgumentType(commandRegistryAccess);
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/BlockPredicateArgumentType$BlockPredicate;", at = @At("HEAD"), cancellable = true)
  private void injectedParse(StringReader stringReader, CallbackInfoReturnable<BlockPredicateArgumentType.BlockPredicate> cir) throws CommandSyntaxException {
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
  private static void injectedGetBlockPredicate(CommandContext<ServerCommandSource> context, String name, CallbackInfoReturnable<Predicate<CachedBlockPosition>> cir) throws CommandSyntaxException {
    final Predicate<CachedBlockPosition> returnValue = cir.getReturnValue();
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
