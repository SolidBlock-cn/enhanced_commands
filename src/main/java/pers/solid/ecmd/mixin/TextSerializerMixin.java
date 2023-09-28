package pers.solid.ecmd.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.JsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.EnhancedTranslatableTextContent;
import pers.solid.ecmd.util.TextUtil;

import java.lang.reflect.Type;

/**
 * 此 mixin 用于实现与 {@link EnhancedTranslatableTextContent} 有关的 JSON 序列化和反序列化过程。{@link EnhancedTranslatableTextContent} 对象会在 JSON 中添加一个 {@code "<mod_id>:enhanced": true}。
 */
@Mixin(Text.Serializer.class)
public abstract class TextSerializerMixin {
  @Unique
  private static final String ENHANCED_KEY = EnhancedCommands.MOD_ID + ":enhanced";

  @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/text/MutableText;", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/JsonHelper;getString(Lcom/google/gson/JsonObject;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", shift = At.Shift.AFTER), slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=fallback"), to = @At(value = "CONSTANT", args = "stringValue=with")))
  public void injectedReadEnhancedBoolean(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext, CallbackInfoReturnable<MutableText> cir, @Local JsonObject jsonObject, @Share("enhanced") LocalBooleanRef localBooleanRef) {
    localBooleanRef.set(JsonHelper.getBoolean(jsonObject, ENHANCED_KEY, false));
  }

  @WrapOperation(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/text/MutableText;", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;translatableWithFallback(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;"), slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=translate"), to = @At(value = "CONSTANT", args = "stringValue=score")))
  public MutableText wrappedSetMutableText(String string, String string2, Object[] objects, Operation<MutableText> original, @Share("enhanced") LocalBooleanRef localBooleanRef) {
    if (localBooleanRef.get()) {
      return TextUtil.enhancedTranslatableWithFallback(string, string2, objects);
    } else {
      return original.call(string, string2, objects);
    }
  }

  @WrapOperation(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/text/MutableText;", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;translatableWithFallback(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/text/MutableText;"), slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=translate"), to = @At(value = "CONSTANT", args = "stringValue=score")))
  public MutableText wrappedSetMutableText(String string, String string2, Operation<MutableText> original, @Share("enhanced") LocalBooleanRef localBooleanRef) {
    if (localBooleanRef.get()) {
      return TextUtil.enhancedTranslatableWithFallback(string, string2);
    } else {
      return original.call(string, string2);
    }
  }

  @Inject(method = "serialize(Lnet/minecraft/text/Text;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;", at = @At(value = "INVOKE", target = "Lcom/google/gson/JsonObject;addProperty(Ljava/lang/String;Ljava/lang/String;)V", remap = false), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/text/TranslatableTextContent;getKey()Ljava/lang/String;"), to = @At(value = "INVOKE", target = "Lnet/minecraft/text/TranslatableTextContent;getFallback()Ljava/lang/String;")))
  public void injectedAddEnhancedProperty(Text text, Type type, JsonSerializationContext jsonSerializationContext, CallbackInfoReturnable<JsonElement> cir, @Local JsonObject jsonObject, @Local TranslatableTextContent translatableTextContent) {
    jsonObject.addProperty(ENHANCED_KEY, translatableTextContent instanceof EnhancedTranslatableTextContent);
  }
}
