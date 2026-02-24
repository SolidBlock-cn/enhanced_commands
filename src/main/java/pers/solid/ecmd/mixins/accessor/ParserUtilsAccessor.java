package pers.solid.ecmd.mixins.accessor;

import com.google.gson.stream.JsonReader;
import net.minecraft.commands.ParserUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ParserUtils.class)
public interface ParserUtilsAccessor {
  @Invoker
  static int invokeGetPos(JsonReader jsonReader) {
    throw new AssertionError();
  }
}
