package pers.solid.ecmd.mixins.accessor;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Optional;

@Mixin(TranslatableContents.class)
public interface TranslatableContentsAccessor {

  @Accessor
  static Codec<Object> getARG_CODEC() {
    throw new UnsupportedOperationException();
  }

  @Invoker
  FormattedText invokeGetArgument(int index);

  @Invoker("adjustArgs")
  static Optional<List<Object>> callToOptionalList(Object[] args) {
    throw new UnsupportedOperationException();
  }
}
