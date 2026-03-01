package pers.solid.ecmd.api.neoforge;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

public class CommandContextHelperImpl {
  public static final @NotNull Field ARGUMENTS_FIELD = FieldUtils.getDeclaredField(CommandContext.class, "arguments", true);

  @SuppressWarnings("unchecked")
  public static <S> Map<String, ParsedArgument<S, ?>> getArgumentsOf(CommandContext<S> commandContext) {
    try {
      return (Map<String, ParsedArgument<S, ?>>) ARGUMENTS_FIELD.get(commandContext);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
