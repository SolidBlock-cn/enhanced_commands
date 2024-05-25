package pers.solid.ecmd.util.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 包含一些常用 codec 的类。
 */
public final class CodecUtil {
  /**
   * 处理正则表达式的 codec，当表达式无效时，返回 error。
   */
  public static final Codec<Pattern> PATTERN = Codec.STRING.flatXmap(s -> {
    try {
      return DataResult.success(Pattern.compile(s));
    } catch (PatternSyntaxException e) {
      return DataResult.error(e::getMessage);
    }
  }, pattern -> DataResult.success(pattern.pattern()));

  public static Codec<Property<?>> propertyForBlock(StateManager<Block, BlockState> stateManager) {
    return Codec.STRING.flatXmap(s -> {
      final Property<?> property = stateManager.getProperty(s);
      if (property == null) {
        return DataResult.error(() -> stateManager.getOwner() + " does not support property named " + s);
      }
      return DataResult.success(property);
    }, property -> DataResult.success(property.getName()));
  }

  public static <A> Codec<Set<A>> set(Codec<A> elementCodec) {
    return new SetCodec<>(elementCodec);
  }

  private CodecUtil() {
  }
}
