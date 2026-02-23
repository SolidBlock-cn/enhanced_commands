package pers.solid.ecmd.mixins.accessor;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(MutableComponent.class)
public interface MutableTextAccessor {
  @Invoker("<init>")
  static MutableComponent createMutableText(ComponentContents content, List<Component> siblings, Style style) {
    throw new UnsupportedOperationException();
  }
}
