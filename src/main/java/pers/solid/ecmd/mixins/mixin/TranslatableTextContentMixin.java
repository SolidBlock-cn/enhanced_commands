package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.dynamic.Codecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.util.EnhancedTranslatableTextContent;

import java.util.Optional;

@Mixin(TranslatableTextContent.class)
public abstract class TranslatableTextContentMixin {
  @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"))
  private static MapCodec<TranslatableTextContent> modifyCodec(MapCodec<TranslatableTextContent> original) {
    return Codecs.optional(Codec.BOOL).xmap(b -> b.orElse(null), o -> o ? Optional.of(true) : Optional.empty()).dispatchMap("enhanced_commands:enhanced", x -> x instanceof EnhancedTranslatableTextContent, bl -> bl ? EnhancedTranslatableTextContent.CODEC : original);
  }
}
