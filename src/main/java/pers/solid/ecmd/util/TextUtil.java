package pers.solid.ecmd.util;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.UnaryOperator;

import static pers.solid.ecmd.util.StringUtil.nf;

/**
 * 和{@linkplain Component 文本组件}有关的实用方法。
 */
public final class TextUtil {

  private TextUtil() {
  }

  /**
   * 将 NBT 转换为可读的字符串。与 {@link net.minecraft.nbt.StringTagVisitor#visit(Tag)} 不同的是，此函数返回的结果中，会在适当的位置添加空格，同时不进行换行，从而确保适当美观，并与 {@link NbtPredicate#asString()} 和 {@link NbtFunction#asString()} 的结果保持一致。
   */
  public static String toSpacedStringNbt(Tag nbtElement) {
    return new SnbtPrinterTagVisitor(StringUtils.EMPTY, 0, new ArrayList<>()).visit(nbtElement);
  }

  public static MutableComponent literal(boolean value) {
    return Component.literal(Boolean.toString(value));
  }

  public static MutableComponent literal(long value) {
    return Component.literal(Long.toString(value));
  }

  public static MutableComponent literal(int value) {
    return Component.literal(Integer.toString(value));
  }

  public static MutableComponent literal(float value) {
    return Component.literal(Float.toString(value));
  }

  public static MutableComponent literal(double value) {
    return Component.literal(Double.toString(value));
  }

  public static MutableComponent literal(ResourceLocation value) {
    return Component.literal(value.toString());
  }

  public static MutableComponent literal(StringRepresentable value) {
    return Component.literal(value.getSerializedName());
  }

  public static MutableComponent literal(ExpressionConvertible value) {
    return Component.literal(value.asString());
  }

  /**
   * 将方块坐标表示为文本组件。
   */
  public static MutableComponent wrapVector(Vec3i blockPos) {
    return Component.translatable("enhanced_commands.position", nf.format(blockPos.getX()), nf.format(blockPos.getY()), nf.format(blockPos.getZ()));
  }

  /**
   * 将坐标表示为文本组件，以用于命令输出。
   */
  public static MutableComponent wrapVector(Position position) {
    return Component.translatable("enhanced_commands.position", nf.format(position.x()), nf.format(position.y()), nf.format(position.z()));
  }

  /**
   * 将方向表示为可翻译的文本组件。
   */
  public static MutableComponent wrapDirection(Direction direction) {
    return Component.translatable("enhanced_commands.direction." + direction.getSerializedName());
  }

  /**
   * 将坐标轴表示为可翻译的文本组件。
   */
  public static MutableComponent wrapAxis(Direction.Axis axis) {
    return Component.translatable("enhanced_commands.axis." + axis.getSerializedName());
  }

  /**
   * 将布尔值表示为文本组件，不翻译但是夫根据其值来应用格式。
   */
  public static MutableComponent wrapBoolean(boolean b) {
    return Component.literal(Boolean.toString(b)).withStyle(b ? ChatFormatting.GREEN : ChatFormatting.RED);
  }

  /**
   * 给文本添加样式，同时避免对文本自身进行复制。如果文本已经有样式，这些样式不会被覆盖。
   */
  public static MutableComponent styled(Component text, UnaryOperator<Style> styleUpdater) {
    return Component.empty().withStyle(styleUpdater).append(text);
  }

  /**
   * 给文本设置颜色样式，同时避免对文本自身进行复制。如果文本已经有样式，这些样式不会被覆盖。
   *
   * @param color 0xAARRGGBB 格式的颜色
   */
  public static MutableComponent styledWithColor(Component text, int color) {
    return styled(text, style -> style.withColor(color));
  }

  /**
   * 组合两部分可能为 {@code null｝ 的文本。如果一个为 {@code null} 另一个未 {@code null}，则直接返回其中的非 ｛@code null} 值。如果两个都不是 {@code null}，将其组合。如果 两个都是 {@code null}，返回空文本。
   */
  @Contract(value = "null, null -> !null; null, !null -> param2; !null, null -> param1", pure = true)
  public static Component joinNullableLines(@Nullable Component text1, @Nullable Component text2) {
    if (text1 == null) {
      return Objects.requireNonNullElseGet(text2, Component::empty);
    } else {
      if (text2 == null) {
        return text1;
      } else {
        return Component.empty().append(text1).append(CommonComponents.NEW_LINE).append(text2);
      }
    }
  }

  public static Component joinNullableLines(@Nullable Component... texts) {
    return CommonComponents.joinLines(Collections2.filter(Arrays.asList(texts), Predicates.notNull()));
  }

  public static MutableComponent biome(ResourceKey<Biome> key) {
    return Component.translatableWithFallback(Util.makeDescriptionId("biome", key.location()), key.location().getPath().replace('_', ' '));
  }
}
