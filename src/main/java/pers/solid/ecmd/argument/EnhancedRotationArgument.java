package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

import java.util.Arrays;
import java.util.Collection;

import static pers.solid.ecmd.util.EnhancedCommandSyntaxException.withCursorEnd;

public enum EnhancedRotationArgument implements ArgumentType<RotationProvider> {
  INSTANCE;
  private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~~", "~-5~5");

  @Override
  public RotationProvider parse(StringReader reader) throws CommandSyntaxException {
    if (!reader.canRead()) {
      throw RotationArgument.ERROR_NOT_COMPLETE.createWithContext(reader);
    } else {

      float[] values = new float[2];
      boolean[] isRelatives = new boolean[2];
      Arrays.fill(isRelatives, false);

      for (int i = 0; i < 2; i++) {
        if (!reader.canRead()) {
          throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(reader);
        }

        boolean hasTilde = false;
        if (reader.peek() == '~') {
          isRelatives[i] = true;
          hasTilde = true;
          reader.skip();
        } else if (reader.peek() == '^') {
          throw withCursorEnd(Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader), reader.getCursor() + 1);
        }

        float num;
        if (reader.canRead() && StringReader.isAllowedNumber(reader.peek()) || !hasTilde) {
          num = reader.readFloat();
        } else {
          num = 0;
        }
        values[i] = num;

        if (i < 1) {
          reader.skipWhitespace();
        }
      }

      return new RotationProvider(values[1], values[0], isRelatives[0], isRelatives[1]);
    }
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
