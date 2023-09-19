package pers.solid.ecmd.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.argument.BlockFunctionArgumentType;
import pers.solid.ecmd.util.mixin.ArgumentTypeExtension;
import pers.solid.ecmd.util.mixin.ForwardedBlockStateArgument;

import java.util.concurrent.CompletableFuture;

/**
 * 此 mixin 用于在原版的 {@link BlockStateArgument} 中实现模组中的 {@link BlockFunctionArgumentType} 功能。
 */
@Mixin(BlockStateArgumentType.class)
public abstract class BlockStateArgumentTypeMixin implements ArgumentTypeExtension {
  @Unique
  private BlockFunctionArgumentType modArgumentType;
  @Unique
  private boolean extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
    this.modArgumentType = new BlockFunctionArgumentType(commandRegistryAccess);
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/BlockStateArgument;", at = @At("HEAD"), cancellable = true)
  private void injectedParse(StringReader stringReader, CallbackInfoReturnable<BlockStateArgument> cir) throws CommandSyntaxException {
    if (modArgumentType != null && extension) {
      cir.setReturnValue(new ForwardedBlockStateArgument(modArgumentType.parse(stringReader)));
    }
  }

  @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
  private <S> void injectedListSuggestions(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    if (modArgumentType != null && extension) {
      cir.setReturnValue(modArgumentType.listSuggestions(context, builder));
    }
  }

  @Inject(method = "getBlockState", at = @At("RETURN"))
  private static void injectedGetBlockState(CommandContext<ServerCommandSource> context, String name, CallbackInfoReturnable<BlockStateArgument> cir) {
    final BlockStateArgument returnValue = cir.getReturnValue();
    if (returnValue instanceof ForwardedBlockStateArgument forwardedBlockStateArgument) {
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
