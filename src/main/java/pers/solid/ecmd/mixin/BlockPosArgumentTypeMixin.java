package pers.solid.ecmd.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
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

@Mixin(BlockPosArgumentType.class)
public abstract class BlockPosArgumentTypeMixin implements ArgumentTypeExtension {
  @Unique
  private EnhancedPosArgumentType modArgumentType;
  @Unique
  private boolean extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(CallbackInfo ci) {
    modArgumentType = new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.INT_ONLY, false);
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/PosArgument;", at = @At("HEAD"), cancellable = true)
  private void injectedParse(StringReader stringReader, CallbackInfoReturnable<PosArgument> cir) throws CommandSyntaxException {
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

  @Inject(method = "getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/server/world/ServerWorld;Ljava/lang/String;)Lnet/minecraft/util/math/BlockPos;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;create()Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", shift = At.Shift.BEFORE), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/argument/BlockPosArgumentType;UNLOADED_EXCEPTION:Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;"), to = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;isInBuildLimit(Lnet/minecraft/util/math/BlockPos;)Z")), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void injectedThrowingUnloaded(CommandContext<ServerCommandSource> context, ServerWorld world, String name, CallbackInfoReturnable<BlockPos> cir, BlockPos blockPos) throws CommandSyntaxException {
    throw EnhancedPosArgumentType.UNLOADED_EXCEPTION.create(TextUtil.wrapBlockPos(blockPos));
  }

  @Inject(method = "getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/server/world/ServerWorld;Ljava/lang/String;)Lnet/minecraft/util/math/BlockPos;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;create()Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", shift = At.Shift.BEFORE), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/argument/BlockPosArgumentType;OUT_OF_WORLD_EXCEPTION:Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;")), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void injectedThrowOutOfWorld(CommandContext<ServerCommandSource> context, ServerWorld world, String name, CallbackInfoReturnable<BlockPos> cir, BlockPos blockPos) throws CommandSyntaxException {
    if (world.isOutOfHeightLimit(blockPos)) {
      throw EnhancedPosArgumentType.OUT_OF_HEIGHT_LIMIT.create(TextUtil.wrapBlockPos(blockPos), world.getBottomY(), world.getTopY());
    } else {
      throw EnhancedPosArgumentType.OUT_OF_BUILD_LIMIT_EXCEPTION.create(TextUtil.wrapBlockPos(blockPos));
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
