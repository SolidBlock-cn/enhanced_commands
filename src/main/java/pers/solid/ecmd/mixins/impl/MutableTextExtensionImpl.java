package pers.solid.ecmd.mixins.impl;

import com.mojang.logging.LogUtils;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.mixins.accessor.MutableTextAccessor;
import pers.solid.ecmd.mixins.ext.MutableTextExtension;
import pers.solid.ecmd.util.EnhancedTranslatableTextContent;

import java.util.List;

/**
 * 此 mixin 用于让 {@link MutableText} 实现 {@link MutableTextExtension}。
 */
@Mixin(MutableText.class)
public abstract class MutableTextExtensionImpl implements MutableTextExtension {
  @Shadow
  @Final
  private TextContent content;

  @Shadow
  @Final
  private List<Text> siblings;

  @Shadow
  private Style style;

  @Override
  public MutableText enhanced$$() {
    if (true) {
      LogUtils.getLogger().info("Called enhanced$$ but I skipped");
      return (MutableText) (Object) this;
    }
    if (content instanceof TranslatableTextContent translatableTextContent) {
      final EnhancedTranslatableTextContent enhancedContent = new EnhancedTranslatableTextContent(translatableTextContent.getKey(), translatableTextContent.getFallback(), translatableTextContent.getArgs());
      return MutableTextAccessor.createMutableText(enhancedContent, siblings, style);
    } else {
      return MutableTextExtension.super.enhanced$$();
    }
  }

  @Inject(method = "of", at = @At("HEAD"))
  private static void headOfOf(TextContent content, CallbackInfoReturnable<MutableText> cir) {
    LogUtils.getLogger().info("Head of 'MutableText.of'!, {}", content);
  }

  @Inject(method = "of", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false))
  private static void newArrayList(TextContent content, CallbackInfoReturnable<MutableText> cir) {
    LogUtils.getLogger().info("newArrayList in 'MutableText.of' called");

    try {
      LogUtils.getLogger().info("Style.EMPTY = {}", Style.EMPTY);
    } catch (Throwable throwable) {
      LogUtils.getLogger().error("Error when referring to Style.EMPTY", throwable);
    }
  }
}
