package pers.solid.ecmd.mixins.ext;

import net.minecraft.network.chat.MutableComponent;
import pers.solid.ecmd.mixins.impl.MutableComponentExtensionImpl;

/**
 * 此命令用于扩展 {@link net.minecraft.network.chat.MutableComponent}。
 *
 * @see MutableComponentExtensionImpl
 */
public interface MutableTextExtension {
  default MutableComponent enhanced$$() {
    throw new UnsupportedOperationException("Only MutableText with TranslatableTextContent can call this method.");
  }
}
