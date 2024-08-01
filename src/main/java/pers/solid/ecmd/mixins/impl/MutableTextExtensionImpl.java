package pers.solid.ecmd.mixins.impl;

import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    if (content instanceof TranslatableTextContent translatableTextContent) {
      final EnhancedTranslatableTextContent enhancedContent = new EnhancedTranslatableTextContent(translatableTextContent.getKey(), translatableTextContent.getFallback(), translatableTextContent.getArgs());
      return MutableTextAccessor.createMutableText(enhancedContent, siblings, style);
    } else {
      return MutableTextExtension.super.enhanced$$();
    }
  }
}
