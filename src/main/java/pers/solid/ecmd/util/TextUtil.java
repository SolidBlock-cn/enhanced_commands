package pers.solid.ecmd.util;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.visitor.NbtOrderedStringFormatter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;

import java.util.ArrayList;
import java.util.function.UnaryOperator;

public final class TextUtil {
  public static final UnaryOperator<Style> STYLE_FOR_TARGET = style -> style.withColor(0x30f5c5);
  public static final UnaryOperator<Style> STYLE_FOR_ACTUAL = style -> style.withColor(0xf5c255);
  public static final UnaryOperator<Style> STYLE_FOR_EXPECTED = style -> style.withColor(0xb8f530);
  public static final UnaryOperator<Style> STYLE_FOR_RESULT = style -> style.withColor(0x7cf3a0);

  private TextUtil() {
  }

  /**
   * 将 NBT 转换为可读的字符串。与 {@link net.minecraft.nbt.visitor.StringNbtWriter#apply(NbtElement)} 不同的是，此函数返回的结果中，会在适当的位置添加空格，同时不进行换行，从而确保适当美观，并与 {@link NbtPredicate#asString()} 和 {@link NbtFunction#asString()} 的结果保持一致。
   */
  public static String toSpacedStringNbt(@NotNull NbtElement nbtElement) {
    return new NbtOrderedStringFormatter(StringUtils.EMPTY, 0, new ArrayList<>()).apply(nbtElement);
  }

  public static MutableText wrapBlockPos(Vec3i blockPos) {
    return Text.translatable("enhancedCommands.vec3i", blockPos.getX(), blockPos.getY(), blockPos.getZ());
  }

  public static MutableText wrapPosition(Position position) {
    return Text.translatable("enhancedCommands.position", position.getX(), position.getY(), position.getZ());
  }

  public static MutableText wrapDirection(Direction direction) {
    return Text.translatable("enhancedCommands.direction." + direction.asString());
  }

  public static MutableText wrapBoolean(boolean b) {
    return Text.literal(Boolean.toString(b)).formatted(b ? Formatting.GREEN : Formatting.RED);
  }

  public static MutableText enhancedTranslatable(String key) {
    return MutableText.of(new EnhancedTranslatableTextContent(key, null, TranslatableTextContent.EMPTY_ARGUMENTS));
  }

  public static MutableText enhancedTranslatable(String key, Object... args) {
    return MutableText.of(new EnhancedTranslatableTextContent(key, null, args));
  }

  public static MutableText enhancedTranslatableWithFallback(String key, @Nullable String fallback) {
    return MutableText.of(new EnhancedTranslatableTextContent(key, fallback, TranslatableTextContent.EMPTY_ARGUMENTS));
  }

  public static MutableText enhancedTranslatableWithFallback(String key, @Nullable String fallback, Object... args) {
    return MutableText.of(new EnhancedTranslatableTextContent(key, fallback, args));
  }
}
