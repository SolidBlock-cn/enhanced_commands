package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.text.Text;

public final class ModCommandExceptionTypes {
  public static final DynamicCommandExceptionType INVALID_REGEX = new DynamicCommandExceptionType(msg -> Text.translatable("enhancedCommands.argument.regex.invalid", msg));

  private ModCommandExceptionTypes() {
  }
}
