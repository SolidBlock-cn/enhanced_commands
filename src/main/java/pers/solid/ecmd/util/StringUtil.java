package pers.solid.ecmd.util;

import net.minecraft.util.math.Position;

/**
 * 与字符串有关的实用类。
 */
public final class StringUtil {
  private StringUtil() {
  }

  public static String wrapPosition(Position position) {
    return position.getX() + " " + position.getY() + " " + position.getZ();
  }
}
