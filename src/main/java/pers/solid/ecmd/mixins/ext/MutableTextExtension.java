package pers.solid.ecmd.mixins.ext;

import net.minecraft.network.chat.MutableComponent;

/**
 * 此命令用于扩展 {@link net.minecraft.network.chat.MutableComponent}。
 *
 * @see pers.solid.ecmd.mixins.impl.MutableTextExtensionImpl
 */
public interface MutableTextExtension {
  default MutableComponent enhanced$$() {
    throw new UnsupportedOperationException("Only MutableText with TranslatableTextContent can call this method.");
  }
}
