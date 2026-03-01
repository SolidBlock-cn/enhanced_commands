package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.config.GeneralParsingConfig;
import pers.solid.ecmd.util.extension.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.stream.Stream;

@Mixin(GameModeArgument.class)
public abstract class GameModeArgumentMixin {

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/level/GameType;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", remap = false))
  public void injectedBeforeUnquotedString(StringReader stringReader, CallbackInfoReturnable<GameType> cir, @Share("cursorBeforeUnquotedString") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/level/GameType;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", shift = At.Shift.AFTER, remap = false))
  public void injectedAfterUnquotedString(StringReader stringReader, CallbackInfoReturnable<GameType> cir, @Share("cursorAfterUnquotedString") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/level/GameType;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameType;byName(Ljava/lang/String;Lnet/minecraft/world/level/GameType;)Lnet/minecraft/world/level/GameType;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  public void injectedParse(StringReader stringReader, CallbackInfoReturnable<GameType> cir, String string) {
    if (GeneralParsingConfig.current.acceptGameModeAlias && MixinShared.EXTENDED_GAME_MODE_NAMES.containsKey(string)) {
      cir.setReturnValue(MixinShared.EXTENDED_GAME_MODE_NAMES.get(string));
    }
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/level/GameType;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  public void injectedException(StringReader stringReader, CallbackInfoReturnable<GameType> cir, @Share("cursorBeforeUnquotedString") LocalIntRef localIntRef) {
    stringReader.setCursor(localIntRef.get());
  }

  @ModifyExpressionValue(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/level/GameType;", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  public CommandSyntaxException modifiedException(CommandSyntaxException commandSyntaxException, @Share("cursorAfterUnquotedString") LocalIntRef localIntRef) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, localIntRef.get());
  }

  @ModifyArg(method = "listSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;suggest(Ljava/util/stream/Stream;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
  public Stream<String> modifiedListSuggestions(Stream<String> candidates) {
    if (GeneralParsingConfig.current.acceptGameModeAlias) {
      return Stream.concat(candidates, MixinShared.EXTENDED_GAME_MODE_NAMES.keySet().stream());
    } else {
      return candidates;
    }
  }
}
