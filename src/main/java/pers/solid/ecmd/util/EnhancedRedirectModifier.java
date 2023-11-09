package pers.solid.ecmd.util;

import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@FunctionalInterface
public interface EnhancedRedirectModifier<S> {
  void modifyArguments(Map<String, ParsedArgument<S, ?>> arguments, @Unmodifiable Map<String, ParsedArgument<S, ?>> previousArguments, S source) throws CommandSyntaxException;

  interface Multiple<S> extends EnhancedRedirectModifier<S>, RedirectModifier<S> {
  }

  interface Constant<S> extends Multiple<S> {
    @Override
    default Collection<S> apply(CommandContext<S> context) throws CommandSyntaxException {
      return Collections.singleton(context.getSource());
    }
  }
}
