package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.util.mixin.TranslatableTextContentMixinHelper;

@Mixin(TranslatableContents.class)
public abstract class TranslatableContentsMixin {
  @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;", remap = false))
  private static MapCodec<TranslatableContents> modifyCodec(MapCodec<TranslatableContents> original) {
    return TranslatableTextContentMixinHelper.modifyTranslatableCodec(original);
  }
}
