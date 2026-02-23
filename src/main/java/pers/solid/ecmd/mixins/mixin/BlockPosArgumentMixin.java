package pers.solid.ecmd.mixins.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.mixin.ArgumentTypeExtension;

import java.util.concurrent.CompletableFuture;

@Mixin(BlockPosArgument.class)
public abstract class BlockPosArgumentMixin implements ArgumentTypeExtension {
  @Unique
  private EnhancedPosArgumentType modArgumentType;
  @Unique
  private boolean extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(CallbackInfo ci) {
    modArgumentType = EnhancedPosArgumentType.blockPos();
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/coordinates/Coordinates;", at = @At("HEAD"), cancellable = true)
  private void injectedParse(StringReader stringReader, CallbackInfoReturnable<Coordinates> cir) throws CommandSyntaxException {
    if (modArgumentType != null && extension) {
      cir.setReturnValue(modArgumentType.parse(stringReader));
    }
  }

  @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
  private <S> void injectedListSuggestions(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    if (modArgumentType != null && extension) {
      cir.setReturnValue(modArgumentType.listSuggestions(context, builder));
    }
  }

  @Inject(method = "getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;create()Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/coordinates/BlockPosArgument;ERROR_NOT_LOADED:Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;"), to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isInWorldBounds(Lnet/minecraft/core/BlockPos;)Z")), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void injectedThrowingUnloaded(CommandContext<CommandSourceStack> context, ServerLevel world, String name, CallbackInfoReturnable<BlockPos> cir, BlockPos blockPos) throws CommandSyntaxException {
    throw EnhancedPosArgumentType.UNLOADED_EXCEPTION.create(TextUtil.wrapVector(blockPos));
  }

  @Inject(method = "getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;create()Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/coordinates/BlockPosArgument;ERROR_OUT_OF_WORLD:Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;", remap = false)), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void injectedThrowOutOfWorld(CommandContext<CommandSourceStack> context, ServerLevel world, String name, CallbackInfoReturnable<BlockPos> cir, BlockPos blockPos) throws CommandSyntaxException {
    throw EnhancedPosArgumentType.OUT_OF_BUILD_LIMIT_EXCEPTION.create(TextUtil.wrapVector(blockPos));
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
