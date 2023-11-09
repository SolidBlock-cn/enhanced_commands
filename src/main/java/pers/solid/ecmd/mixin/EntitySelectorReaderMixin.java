package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.StringReader;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;
import pers.solid.ecmd.util.mixin.EntitySelectorExtension;
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

  @ModifyExpressionValue(method = "readAtVariable", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;read()C", remap = false))
  private char setAtVariable(char c) {
    ec$ext.atVariable = Character.toString(c);
    return c;
  }

  @Inject(method = "readAtVariable", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setEntityType(Lnet/minecraft/entity/EntityType;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private void setImplicitEntityType(CallbackInfo ci, int i, char c) {
    if (c != 'a') {
      ec$ext.implicitEntityType = true;
      ec$ext.implicitNonPlayers = true;
    }
  }

  /**
   * 在结束时对对象进行修改，使之接受本模组中的受实体命令源影响的谓词。
   */
  @Inject(method = "build", at = @At("RETURN"))
  private void buildExtraPredicate(CallbackInfoReturnable<EntitySelector> cir) {
    final EntitySelector returnValue = cir.getReturnValue();
    ((EntitySelectorExtension) returnValue).ec$getExt().predicateFunctions.addAll(ec$ext.predicateFunctions);
  }

  /**
   * 如果通过 {@code gamemode}、{@code level} 等选项将 {@link EntitySelectorReader#setIncludesNonPlayers(boolean)} 为 {@code false}，那么 {@code implicitNonPlayers} 就应该是 {@code false}。需要注意的是，在 {@link EntitySelectorReader#readArguments()} 中读取 {@code @p} 等参数时，是直接修改的字段，没有调用此方法。
   */
  @Inject(method = "setIncludesNonPlayers", at = @At("TAIL"))
  private void setExplicitNonPlayer(boolean includesNonPlayers, CallbackInfo ci) {
    ec$ext.implicitNonPlayers = false;
  }
}