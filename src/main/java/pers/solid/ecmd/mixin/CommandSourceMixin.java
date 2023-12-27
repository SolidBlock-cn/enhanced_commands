package pers.solid.ecmd.mixin;

import com.google.common.base.Functions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pers.solid.ecmd.configs.GeneralParsingConfig;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(CommandSource.class)
public interface CommandSourceMixin {
  @SuppressWarnings("Contract")
  @Shadow
  @Contract("_, _ -> _")
  static boolean shouldSuggest(String remaining, String candidate) {
    throw new AssertionError();
  }

  @ModifyArg(method = "suggestIdentifiers(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;forEachMatching(Ljava/lang/Iterable;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"), index = 3)
  private static Consumer<Identifier> suggestIdentifiersEmitNamespace(Consumer<Identifier> action, @Local String remaining, @Local(argsOnly = true) SuggestionsBuilder builder) {
    return getModifiedConsumer(Functions.identity(), action, remaining, (identifier, identifier2) -> builder.suggest(identifier.getPath()));
  }

  @ModifyArg(method = "suggestIdentifiers(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;forEachMatching(Ljava/lang/Iterable;Ljava/lang/String;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"), index = 4)
  private static Consumer<Identifier> suggestIdentifiersEmitNamespaceWithPrefix(Consumer<Identifier> action, @Local(ordinal = 1) String remaining, @Local(argsOnly = true) SuggestionsBuilder builder, @Local(argsOnly = true) String prefix) {
    return getModifiedConsumer(Functions.identity(), action, remaining, (identifier, identifier2) -> builder.suggest(prefix + identifier.getPath()));
  }

  @ModifyArg(method = "suggestFromIdentifier(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandSource;forEachMatching(Ljava/lang/Iterable;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"), index = 3)
  private static <T> Consumer<T> suggestFromIdentifiers(Consumer<T> action, @Local String remaining, @Local(argsOnly = true) SuggestionsBuilder builder, @Local(argsOnly = true, ordinal = 0) Function<T, Identifier> identifier, @Local(argsOnly = true, ordinal = 1) Function<T, Message> tooltip) {
    return getModifiedConsumer(identifier, action, remaining, (identifier1, t) -> builder.suggest(identifier1.getPath(), tooltip.apply(t)));
  }

  /**
   * 在输入了不含冒号的 id 时，除了建议可能以此为开头的 id 之外，还会建议原版命名空间中的 id，此情况下可以省略命名空间。例如，输入了 {@code dir} 时，建议 {@code dirt} 而非 {@code minecraft:dirt}。
   */
  @Unique
  private static <T> Consumer<T> getModifiedConsumer(Function<T, Identifier> identifierFunction, Consumer<T> action, String remaining, BiConsumer<Identifier, T> modifiedSuggestion) {
    if (GeneralParsingConfig.CURRENT.suggestionEmitDefaultNamespace) {
      if (remaining.indexOf(':') == -1) {
        return t -> {
          final Identifier identifier = identifierFunction.apply(t);
          if (identifier.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) && (remaining.isEmpty() || !shouldSuggest(remaining, Identifier.DEFAULT_NAMESPACE))) {
            modifiedSuggestion.accept(identifier, t);
          } else {
            action.accept(t);
          }
        };
      }
    }

    return action;
  }

  /**
   * 在提供 id 的建议时，即使 id 不是原版默认命令空间的，也允许提供相应建议。例如，输入了 {@code path} 时，可建议 {@code minecraft:path} 和 {@code non_minecraft_namespace:path}。
   */
  @ModifyExpressionValue(method = "forEachMatching(Ljava/lang/Iterable;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
  private static boolean suggestNonDefaultNamespacedIds(boolean original) {
    return original || GeneralParsingConfig.CURRENT.suggestNonDefaultNamespacedIds;
  }
}
