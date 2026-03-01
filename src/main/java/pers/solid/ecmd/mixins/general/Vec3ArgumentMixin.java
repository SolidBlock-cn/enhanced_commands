package pers.solid.ecmd.mixins.general;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.mixin.ArgumentTypeExtension;

import java.util.concurrent.CompletableFuture;

@Mixin(Vec3Argument.class)
public abstract class Vec3ArgumentMixin implements ArgumentTypeExtension {
  @Unique
  private EnhancedPosArgument modArgumentType;
  @Unique
  private boolean extension = true;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void injectedInit(boolean centerIntegers, CallbackInfo ci) {
    modArgumentType = new EnhancedPosArgument(EnhancedPosArgument.NumberType.PREFER_DOUBLE, centerIntegers ? EnhancedPosArgument.IntAlignType.HORIZONTALLY_CENTERED : EnhancedPosArgument.IntAlignType.UNCHANGED);
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

  @Override
  public boolean enhanced_hasExtension() {
    return extension;
  }

  @Override
  public void enhanced_setExtension(boolean extension) {
    this.extension = extension;
  }
}
