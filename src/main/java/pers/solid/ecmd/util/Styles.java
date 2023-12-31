package pers.solid.ecmd.util;

import net.minecraft.text.Style;

import java.awt.*;
import java.util.function.UnaryOperator;

public final class Styles {
  private static final Color TARGET_COLOR = new Color(0xDD95F5);
  private static final Color ACTUAL_COLOR = new Color(0x58B8FF);
  private static final Color EXPECTED_COLOR = new Color(0x66F6AD);
  private static final Color RESULT_COLOR = new Color(0x56FFF8);
  /**
   * 命令输出结果中的目标部分的样式。
   */
  public static final UnaryOperator<Style> TARGET = style -> style.withColor(TARGET_COLOR.getRGB());
  /**
   * 命令输出结果中获得到的实际值部分的样式。
   */
  public static final UnaryOperator<Style> ACTUAL = style -> style.withColor(ACTUAL_COLOR.getRGB());
  /**
   * 命令输出结果中的预期值（可能与实际值不同）的部分的样式。
   */
  public static final UnaryOperator<Style> EXPECTED = style -> style.withColor(EXPECTED_COLOR.getRGB());
  /**
   * 命令输出结果中表示计算或者运行的结果中的部分的样式。
   */
  public static final UnaryOperator<Style> RESULT = style -> style.withColor(RESULT_COLOR.getRGB());
  private static final Color TRUE_COLOR = new Color(0x66FA68);
  private static final Color FALSE_COLOR = new Color(0xFC7E7E);
  private static final Color MEDIUM_COLOR = new Color(0xF8ED7E);
  public static final UnaryOperator<Style> TRUE = style -> style.withColor(TRUE_COLOR.getRGB());
  public static final UnaryOperator<Style> FALSE = style -> style.withColor(FALSE_COLOR.getRGB());
  public static final UnaryOperator<Style> MEDIUM = style -> style.withColor(MEDIUM_COLOR.getRGB());

  public static UnaryOperator<Style> trueOrFalse(boolean value) {
    return value ? TRUE : FALSE;
  }

  private Styles() {
  }
}
