package pers.solid.ecmd.mixin;

import com.google.gson.stream.JsonReader;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Text.Serializer.class)
public interface TextSerializerAccessor {
  @Invoker
  static int invokeGetPosition(JsonReader jsonReader) {
    throw new AssertionError();
  }
}
