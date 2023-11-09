package pers.solid.ecmd.mixin;

import com.mojang.brigadier.StringReader;
import net.minecraft.command.EntitySelectorReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;
import pers.solid.ecmd.util.mixin.EntitySelectorReaderExtension;

@Mixin(EntitySelectorReader.class)
public class EntitySelectorReaderMixin implements EntitySelectorReaderExtension {
  @Shadow
  @Final
  private StringReader reader;
  @Unique
  private final EntitySelectorReaderExtras ec$ext = new EntitySelectorReaderExtras();

  @Override
  public EntitySelectorReaderExtras ec$getExt() {
    return ec$ext;
  }

  @Inject(method = "readArguments", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readString()Ljava/lang/String;", remap = false), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorOptions;getHandler(Lnet/minecraft/command/EntitySelectorReader;Ljava/lang/String;I)Lnet/minecraft/command/EntitySelectorOptions$SelectorHandler;")), locals = LocalCapture.CAPTURE_FAILSOFT)
  private void setCursorBeforeOptionName(CallbackInfo ci, int i) {
    ec$ext.cursorBeforeOptionName = i; // cursor
  }

  @Inject(method = "readArguments", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorOptions;getHandler(Lnet/minecraft/command/EntitySelectorReader;Ljava/lang/String;I)Lnet/minecraft/command/EntitySelectorOptions$SelectorHandler;"))
  private void setCursorAfterOptionName(CallbackInfo ci) {
    ec$ext.cursorAfterOptionName = reader.getCursor(); // cursor
  }

  @Inject(method = "readAtVariable", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;read()C", remap = false, shift = At.Shift.BY, by = 2), locals = LocalCapture.CAPTURE_FAILSOFT)
  private void setAtVariable(CallbackInfo ci, int i, char c) {
    ec$ext.atVariable = Character.toString(c);
  }

  @Inject(method = "readAtVariable", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setEntityType(Lnet/minecraft/entity/EntityType;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private void setImplicitEntityType(CallbackInfo ci, int i, char c) {
    if (c != 'a') {
      ec$ext.implicitEntityType = true;
      ec$ext.implicitNonPlayers = true;
    }
  }


  /**
   * 如果通过 {@code gamemode}、{@code level} 等选项将 {@link EntitySelectorReader#setIncludesNonPlayers(boolean)} 为 {@code false}，那么 {@code implicitNonPlayers} 就应该是 {@code false}。需要注意的是，在 {@link EntitySelectorReader#readArguments()} 中读取 {@code @p} 等参数时，是直接修改的字段，没有调用此方法。
   */
  @Inject(method = "setIncludesNonPlayers", at = @At("TAIL"))
  private void setExplicitNonPlayer(boolean includesNonPlayers, CallbackInfo ci) {
    ec$ext.implicitNonPlayers = false;
  }
}
