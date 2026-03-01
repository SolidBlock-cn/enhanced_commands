package pers.solid.ecmd.mixins.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pers.solid.ecmd.mixins.accessor.MutableComponentAccessor;
import pers.solid.ecmd.util.EnhancedTranslatableTextContent;
import pers.solid.ecmd.util.extension.MutableComponentExtension;

import java.util.List;

/**
 * 此 mixin 用于让 {@link MutableComponent} 实现 {@link MutableComponentExtension}。
 */
@Mixin(MutableComponent.class)
public abstract class MutableComponentExtensionImpl implements MutableComponentExtension {
  @Shadow
  @Final
  private ComponentContents contents;

  @Shadow
  @Final
  private List<Component> siblings;

  @Shadow
  private Style style;

  @Override
  public MutableComponent enhanced$$() {
    if (contents instanceof TranslatableContents translatableTextContent) {
      final EnhancedTranslatableTextContent enhancedContent = new EnhancedTranslatableTextContent(translatableTextContent.getKey(), translatableTextContent.getFallback(), translatableTextContent.getArgs());
      return MutableComponentAccessor.createMutableText(enhancedContent, siblings, style);
    } else {
      return MutableComponentExtension.super.enhanced$$();
    }
  }
}
