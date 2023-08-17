package pers.solid.ecmd.util;

import com.mojang.brigadier.StringReader;
import org.jetbrains.annotations.NotNull;

public final class StringUtil {
  private StringUtil() {
  }

  /**
   * @see StringReader#isAllowedInUnquotedString(char)
   */
  public static boolean isAllowedInUnquotedString(final @NotNull String s) {
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (!StringReader.isAllowedInUnquotedString(c))
        return false;
    }
    return true;
  }
}
