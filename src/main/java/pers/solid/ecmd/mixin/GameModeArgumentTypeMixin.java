package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.stream.Stream;

@Mixin(GameModeArgumentType.class)
public abstract class GameModeArgumentTypeMixin {

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/GameMode;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", shift = At.Shift.BEFORE, remap = false))
  public void injectedBeforeUnquotedString(StringReader stringReader, CallbackInfoReturnable<GameMode> cir, @Share("cursorBeforeUnquotedString") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/GameMode;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", shift = At.Shift.AFTER, remap = false))
  public void injectedAfterUnquotedString(StringReader stringReader, CallbackInfoReturnable<GameMode> cir, @Share("cursorAfterUnquotedString") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/GameMode;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameMode;byName(Ljava/lang/String;Lnet/minecraft/world/GameMode;)Lnet/minecraft/world/GameMode;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  public void injectedParse(StringReader stringReader, CallbackInfoReturnable<GameMode> cir, String string) {
    if (MixinShared.EXTENDED_GAME_MODE_NAMES.containsKey(string)) {
      cir.setReturnValue(MixinShared.EXTENDED_GAME_MODE_NAMES.get(string));
    }
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/GameMode;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", shift = At.Shift.BEFORE, remap = false))
  public void injectedException(StringReader stringReader, CallbackInfoReturnable<GameMode> cir, @Share("cursorBeforeUnquotedString") LocalIntRef localIntRef) {
    stringReader.setCursor(localIntRef.get());
  }

  @ModifyExpressionValue(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/GameMode;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  public CommandSyntaxException modifiedException(CommandSyntaxException commandSyntaxException, @Share("cursorAfterUnquotedString") LocalIntRef localIntRef) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, localIntRef.get());
  }

  @ModifyArg(method = "listSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;suggestMatching(Ljava/util/stream/Stream;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
  public Stream<String> modifiedListSuggestions(Stream<String> candidates) {
    return Stream.concat(candidates, MixinShared.EXTENDED_GAME_MODE_NAMES.keySet().stream());
  }
}
