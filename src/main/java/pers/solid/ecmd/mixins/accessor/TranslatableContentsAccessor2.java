package pers.solid.ecmd.mixins.accessor;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Optional;

@Mixin(TranslatableContents.class)
public interface TranslatableContentsAccessor2 {
  @Invoker("adjustArgs")
  static Object[] callToArray(Optional<List<Object>> args) {
    throw new UnsupportedOperationException();
  }
}
