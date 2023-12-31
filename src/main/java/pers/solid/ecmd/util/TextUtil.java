package pers.solid.ecmd.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.visitor.NbtOrderedStringFormatter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.UnaryOperator;

/**
 * 和{@linkplain Text 文本组件}有关的实用方法。
 */
public final class TextUtil {
  /**
   * 命令输出结果中的目标部分的样式。
   */
  public static final UnaryOperator<Style> STYLE_FOR_TARGET = style -> style.withColor(0x30f5c5);
  /**
   * 命令输出结果中获得到的实际值部分的样式。
   */
  public static final UnaryOperator<Style> STYLE_FOR_ACTUAL = style -> style.withColor(0xf5c255);
  /**
   * 命令输出结果中的预期值（可能与实际值不同）的部分的样式。
   */
  public static final UnaryOperator<Style> STYLE_FOR_EXPECTED = style -> style.withColor(0xb8f530);
  /**
   * 命令输出结果中表示计算或者运行的结果中的部分的样式。
   */
  public static final UnaryOperator<Style> STYLE_FOR_RESULT = style -> style.withColor(0x7cf3a0);

  private TextUtil() {
  }

  /**
   * 将 NBT 转换为可读的字符串。与 {@link net.minecraft.nbt.visitor.StringNbtWriter#apply(NbtElement)} 不同的是，此函数返回的结果中，会在适当的位置添加空格，同时不进行换行，从而确保适当美观，并与 {@link NbtPredicate#asString()} 和 {@link NbtFunction#asString()} 的结果保持一致。
   */
  public static String toSpacedStringNbt(@NotNull NbtElement nbtElement) {
    return new NbtOrderedStringFormatter(StringUtils.EMPTY, 0, new ArrayList<>()).apply(nbtElement);
  }

  public static MutableText literal(boolean value) {
    return Text.literal(Boolean.toString(value));
  }

  public static MutableText literal(long value) {
    return Text.literal(Long.toString(value));
  }

  public static MutableText literal(int value) {
    return Text.literal(Integer.toString(value));
  }

  public static MutableText literal(float value) {
    return Text.literal(Float.toString(value));
  }

  public static MutableText literal(double value) {
    return Text.literal(Double.toString(value));
  }

  public static MutableText literal(Identifier value) {
    return Text.literal(value.toString());
  }

  public static MutableText literal(StringIdentifiable value) {
    return Text.literal(value.asString());
  }

  public static MutableText literal(ExpressionConvertible value) {
    return Text.literal(value.asString());
  }

  /**
   * 将方块坐标表示为文本组件。
   */
  public static MutableText wrapVector(Vec3i blockPos) {
    return Text.translatable("enhanced_commands.position", blockPos.getX(), blockPos.getY(), blockPos.getZ());
  }

  /**
   * 将坐标表示为文本组件，以用于命令输出。
   */
  public static MutableText wrapVector(Position position) {
    return Text.translatable("enhanced_commands.position", position.getX(), position.getY(), position.getZ());
  }

  /**
   * 将方向表示为可翻译的文本组件。
   */
  public static MutableText wrapDirection(Direction direction) {
    return Text.translatable("enhanced_commands.direction." + direction.asString());
  }

  /**
   * 将坐标轴表示为可翻译的文本组件。
   */
  public static MutableText wrapAxis(Direction.Axis axis) {
    return Text.translatable("enhanced_commands.axis." + axis.asString());
  }

  /**
   * 将布尔值表示为文本组件，不翻译但是夫根据其值来应用格式。
   */
  public static MutableText wrapBoolean(boolean b) {
    return Text.literal(Boolean.toString(b)).formatted(b ? Formatting.GREEN : Formatting.RED);
  }

  /**
   * 可翻译并使用增强功能的文本组件，相比 {@link Text#translatable} 有增强的功能。
   */
  public static MutableText enhancedTranslatable(String key) {
    return MutableText.of(new EnhancedTranslatableTextContent(key, null, TranslatableTextContent.EMPTY_ARGUMENTS));
  }

  /**
   * 可翻译并使用增强功能的文本组件，相比 {@link Text#translatable} 有增强的功能。
   */
  public static MutableText enhancedTranslatable(String key, Object... args) {
    return MutableText.of(new EnhancedTranslatableTextContent(key, null, args));
  }

  /**
   * 可翻译并使用增强功能的文本组件，相比 {@link Text#translatable} 有增强的功能。
   */
  public static MutableText enhancedTranslatableWithFallback(String key, @Nullable String fallback) {
    return MutableText.of(new EnhancedTranslatableTextContent(key, fallback, TranslatableTextContent.EMPTY_ARGUMENTS));
  }

  /**
   * 可翻译并使用增强功能的文本组件，相比 {@link Text#translatable} 有增强的功能。
   */
  public static MutableText enhancedTranslatableWithFallback(String key, @Nullable String fallback, Object... args) {
    return MutableText.of(new EnhancedTranslatableTextContent(key, fallback, args));
  }

  /**
   * 给文本添加样式，同时避免对文本自身进行复制。如果文本已经有样式，这些样式不会被覆盖。
   */
  public static MutableText styled(Text text, UnaryOperator<Style> styleUpdater) {
    return Text.empty().styled(styleUpdater).append(text);
  }

  /**
   * 组合两部分可能为 {@code null｝ 的文本。如果一个为 {@code null} 另一个未 {@code null}，则直接返回其中的非 ｛@code null} 值。如果两个都不是 {@code null}，将其组合。如果 两个都是 {@code null}，返回空文本。
   */
  @Contract(value = "null, null -> !null; null, !null -> param2; !null, null -> param1", pure = true)
  public static Text joinNullableLines(@Nullable Text text1, @Nullable Text text2) {
    if (text1 == null) {
      if (text2 == null) {
        return Text.empty();
      } else {
        return text2;
      }
    } else {
      if (text2 == null) {
        return text1;
      } else {
        return Text.empty().append(text1).append(ScreenTexts.LINE_BREAK).append(text2);
      }
    }
  }

  public static Text joinNullableLines(@Nullable Text... texts) {
    return ScreenTexts.joinLines(Collections2.filter(Arrays.asList(texts), Predicates.notNull()));
  }

  @NotNull
  public static MutableText biome(RegistryKey<Biome> key) {
    return Text.translatableWithFallback(Util.createTranslationKey("biome", key.getValue()), key.getValue().getPath().replace('_', ' '));
  }
}
