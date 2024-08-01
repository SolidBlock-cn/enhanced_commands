package pers.solid.ecmd.mixins.accessor;

import com.mojang.serialization.Codec;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Optional;

@Mixin(TranslatableTextContent.class)
public interface TranslatableTextContentAccessor {
  @Invoker
  static Optional<List<Object>> callToOptionalList(Object[] args) {
    throw new UnsupportedOperationException();
  }

  @Accessor
  static Codec<Object> getARGUMENT_CODEC() {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static Object[] callToArray(Optional<List<Object>> args) {
    throw new UnsupportedOperationException();
  }
}
