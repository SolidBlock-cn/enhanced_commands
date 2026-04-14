package pers.solid.ecmd.mixins.general;

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
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.mixin.ArgumentTypeExtension;

import java.util.concurrent.CompletableFuture;

@Mixin(BlockPosArgument.class)
public abstract class BlockPosArgumentMixin implements ArgumentTypeExtension {
  @Unique
  private @Nullable EnhancedPosArgument enhanced_commands$modArgumentType;
  @Unique
  private boolean enhanced_commands$extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(CallbackInfo ci) {
    enhanced_commands$modArgumentType = EnhancedPosArgument.blockPos();
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/coordinates/Coordinates;", at = @At("HEAD"), cancellable = true)
  private void injectedParse(StringReader reader, CallbackInfoReturnable<Coordinates> cir) throws CommandSyntaxException {
    if (enhanced_commands$modArgumentType != null && enhanced_commands$extension) {
      cir.setReturnValue(enhanced_commands$modArgumentType.parse(reader));
    }
  }

  @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
  private <S> void injectedListSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    if (enhanced_commands$modArgumentType != null && enhanced_commands$extension) {
      cir.setReturnValue(enhanced_commands$modArgumentType.listSuggestions(commandContext, suggestionsBuilder));
    }
  }

  @Inject(method = "getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;create()Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/coordinates/BlockPosArgument;ERROR_NOT_LOADED:Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;", opcode = Opcodes.GETSTATIC), to = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/coordinates/BlockPosArgument;ERROR_OUT_OF_WORLD:Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;", opcode = Opcodes.GETSTATIC)), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void injectedThrowingUnloaded(CommandContext<CommandSourceStack> context, ServerLevel level, String name, CallbackInfoReturnable<BlockPos> cir, BlockPos blockPos) throws CommandSyntaxException {
    throw EnhancedPosArgument.UNLOADED_EXCEPTION.create(TextUtil.wrapVector(blockPos));
  }

  @Inject(method = "getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/server/level/ServerLevel;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;create()Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/coordinates/BlockPosArgument;ERROR_OUT_OF_WORLD:Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;", remap = false, opcode = Opcodes.GETSTATIC)), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void injectedThrowOutOfWorld(CommandContext<CommandSourceStack> context, ServerLevel level, String name, CallbackInfoReturnable<BlockPos> cir, BlockPos blockPos) throws CommandSyntaxException {
    throw EnhancedPosArgument.OUT_OF_BUILD_LIMIT_EXCEPTION.create(TextUtil.wrapVector(blockPos));
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
