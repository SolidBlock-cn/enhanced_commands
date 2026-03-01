package pers.solid.ecmd.api.fabric;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import pers.solid.ecmd.mixins.fabric.CommandContextAccessor;

import java.util.Map;

public class CommandContextHelperImpl {
  @SuppressWarnings("unchecked")
  public static <S> Map<String, ParsedArgument<S, ?>> getArgumentsOf(CommandContext<S> commandContext) {
    return ((CommandContextAccessor<S>) commandContext).getArguments();
  }
}
