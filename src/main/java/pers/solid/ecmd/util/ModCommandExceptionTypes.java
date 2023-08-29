package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.text.Text;

public final class ModCommandExceptionTypes {
  public static final DynamicCommandExceptionType INVALID_REGEX = new DynamicCommandExceptionType(msg -> Text.translatable("enhancedCommands.argument.regex.invalid", msg));
  public static final Dynamic2CommandExceptionType FEATURE_REQUIRED = new Dynamic2CommandExceptionType((blockId, blockName) -> Text.translatable("enhancedCommands.argument.block.feature_required", blockId, blockName));

  private ModCommandExceptionTypes() {
  }
}
