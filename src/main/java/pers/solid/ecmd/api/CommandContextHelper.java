package pers.solid.ecmd.api;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.Map;

public final class CommandContextHelper {
  @ExpectPlatform
  public static <S> Map<String, ParsedArgument<S, ?>> getArgumentsOf(CommandContext<S> commandContext) {
    throw new AssertionError();
  }
}
