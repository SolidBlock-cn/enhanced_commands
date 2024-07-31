package pers.solid.ecmd.mixin;

import com.google.gson.stream.JsonReader;
import net.minecraft.util.JsonReaderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(JsonReaderUtils.class)
public interface JsonReaderUtilsAccessor {
  @Invoker
  static int invokeGetPos(JsonReader jsonReader) {
    throw new AssertionError();
  }
}
