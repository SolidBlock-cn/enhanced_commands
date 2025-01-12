package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.util.mixin.TranslatableTextContentMixinHelper;

@Mixin(TranslatableTextContent.class)
public abstract class TranslatableTextContentMixin {
  @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;", remap = false))
  private static MapCodec<TranslatableTextContent> modifyCodec(MapCodec<TranslatableTextContent> original) {
    if (true) {
      LogUtils.getLogger().info("Tried modify translatable text content codec but I skipped");
      return original;
    }
    return TranslatableTextContentMixinHelper.modifyTranslatableCodec(original);
  }
}
