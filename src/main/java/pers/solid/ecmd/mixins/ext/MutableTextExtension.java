package pers.solid.ecmd.mixins.ext;

import net.minecraft.text.MutableText;

/**
 * 此命令用于扩展 {@link net.minecraft.text.MutableText}。
 *
 * @see pers.solid.ecmd.mixins.impl.MutableTextExtensionImpl
 */
public interface MutableTextExtension {
  default MutableText enhanced$$() {
    throw new UnsupportedOperationException("Only MutableText with TranslatableTextContent can call this method.");
  }
}
