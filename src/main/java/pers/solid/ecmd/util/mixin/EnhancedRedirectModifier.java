package pers.solid.ecmd.util.mixin;

import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.SingleRedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface EnhancedRedirectModifier<S> extends RedirectModifier<S> {
  void modifyArguments(Map<String, ParsedArgument<S, ?>> arguments, S source) throws CommandSyntaxException;

  static @NotNull <S> EnhancedRedirectModifier<S> of(@NotNull ArgumentsModifier<S> argumentsModifier) {
    return new EnhancedRedirectModifier<>() {
      @Override
      public void modifyArguments(Map<String, ParsedArgument<S, ?>> arguments, S source) throws CommandSyntaxException {
        argumentsModifier.modifyArguments(arguments, source);
      }

      @Override
      public Collection<S> apply(CommandContext<S> context) {
        return Collections.singleton(context.getSource());
      }
    };
  }

  static @NotNull <S> EnhancedRedirectModifier<S> of(@NotNull RedirectModifier<S> redirectModifier, ArgumentsModifier<S> argumentsModifier) {
    return new EnhancedRedirectModifier<>() {
      @Override
      public void modifyArguments(Map<String, ParsedArgument<S, ?>> arguments, S source) throws CommandSyntaxException {
        argumentsModifier.modifyArguments(arguments, source);
      }

      @Override
      public Collection<S> apply(CommandContext<S> context) throws CommandSyntaxException {
        return redirectModifier.apply(context);
      }
    };
  }

  static @NotNull <S> EnhancedRedirectModifier<S> of(@NotNull SingleRedirectModifier<S> singleRedirectModifier, ArgumentsModifier<S> argumentsModifier) {
    return new EnhancedRedirectModifier<>() {
      @Override
      public void modifyArguments(Map<String, ParsedArgument<S, ?>> arguments, S source) throws CommandSyntaxException {
        argumentsModifier.modifyArguments(arguments, source);
      }

      @Override
      public Collection<S> apply(CommandContext<S> context) throws CommandSyntaxException {
        return Collections.singleton(singleRedirectModifier.apply(context));
      }
    };
  }

  interface ArgumentsModifier<S> {
    void modifyArguments(Map<String, ParsedArgument<S, ?>> arguments, S source) throws CommandSyntaxException;
  }
}
