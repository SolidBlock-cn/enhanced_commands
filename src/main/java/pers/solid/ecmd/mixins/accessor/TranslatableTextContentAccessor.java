package pers.solid.ecmd.mixins.accessor;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Optional;

@Mixin(TranslatableContents.class)
public interface TranslatableTextContentAccessor {
  @Invoker
  static Optional<List<Object>> callAdjustArgs(Object[] args) {
    throw new UnsupportedOperationException();
  }

  @Accessor
  static Codec<Object> getARG_CODEC() {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static Object[] callAdjustArgs(Optional<List<Object>> args) {
    throw new UnsupportedOperationException();
  }
}
