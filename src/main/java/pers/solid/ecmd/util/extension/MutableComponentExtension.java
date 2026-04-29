package pers.solid.ecmd.util.extension;

import net.minecraft.network.chat.MutableComponent;
import pers.solid.ecmd.mixins.impl.MutableComponentExtensionImpl;

/**
 * 此命令用于扩展 {@link net.minecraft.network.chat.MutableComponent}。
 *
 * @see MutableComponentExtensionImpl
 */
public interface MutableComponentExtension {
  default MutableComponent enhanced$$() {
    throw new UnsupportedOperationException("Only MutableComponent can call this method.");
  }
}
