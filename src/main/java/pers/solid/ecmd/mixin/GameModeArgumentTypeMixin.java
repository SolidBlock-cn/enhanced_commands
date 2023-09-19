package pers.solid.ecmd.mixin;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.StringReader;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.stream.Stream;

@Mixin(GameModeArgumentType.class)
public abstract class GameModeArgumentTypeMixin {
  @Unique
  private static final ImmutableMap<String, GameMode> NAME_TO_MODE = ImmutableMap.of(
      "s", GameMode.SURVIVAL,
      "c", GameMode.CREATIVE,
      "a", GameMode.ADVENTURE,
      "sp", GameMode.SPECTATOR,
      "0", GameMode.SURVIVAL,
      "1", GameMode.CREATIVE,
      "2", GameMode.ADVENTURE,
      "3", GameMode.SPECTATOR);

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/world/GameMode;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameMode;byName(Ljava/lang/String;Lnet/minecraft/world/GameMode;)Lnet/minecraft/world/GameMode;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  public void injectedParse(StringReader stringReader, CallbackInfoReturnable<GameMode> cir, String string) {
    if (NAME_TO_MODE.containsKey(string)) {
      cir.setReturnValue(NAME_TO_MODE.get(string));
    }
  }

  @ModifyArg(method = "listSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;suggestMatching(Ljava/util/stream/Stream;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
  public <S> Stream<String> injectedListSuggestions(Stream<String> candidates) {
    return Stream.concat(candidates, NAME_TO_MODE.keySet().stream());
  }
}
