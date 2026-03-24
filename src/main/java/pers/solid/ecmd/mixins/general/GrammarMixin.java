package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.parsing.packrat.ErrorEntry;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.parse.EnhancedSuggestionSupplier;
import pers.solid.ecmd.parse.ParseContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(Grammar.class)
public abstract class GrammarMixin {
  @Unique
  private static final String SHARE_NAME = "enhancedSuggestion";

  @Inject(method = "parseForSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/parsing/packrat/ErrorCollector$LongestOnly;entries()Ljava/util/List;"))
  private void initExtraSuggestionSupplierList(SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir, @Share(SHARE_NAME) LocalRef<List<CompletableFuture<Suggestions>>> share) {
    share.set(null); // 使用 null 而非空列表，以避免每次都创建对象
  }

  /**
   * 用于使 Packrat 的解析器支持自定义的 {@link EnhancedSuggestionSupplier}，使其获取额外建议，并如果有值，将其与已有的建议合并。
   */
  @Inject(method = "parseForSuggestions", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;suggest(Ljava/util/stream/Stream;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
  private void replacedSuggestCall(SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir, @Local ErrorEntry<StringReader> errorEntry, @Share(SHARE_NAME) LocalRef<List<CompletableFuture<Suggestions>>> share) {
    // 不会取消原行为，因为 EnhancedSuggestionSupplier 会通过原版途径提供空建议。
    if (errorEntry.suggestions() instanceof EnhancedSuggestionSupplier<?> enhancedSuggestionSupplier) {

      CompletableFuture<Suggestions> suggestion;
      try {
        suggestion = enhancedSuggestionSupplier.forceGetSuggestionsUnchecked(builder);
      } catch (CommandSyntaxException e) {
        suggestion = null;
      }

      if (suggestion != null) {
        List<CompletableFuture<Suggestions>> list = share.get();
        if (list == null) {
          share.set(list = new ArrayList<>());
        }
        list.add(suggestion);
      }
    }
  }

  /**
   * 将已获取到的额外建议与通过常规方式得到的建议合并。
   */
  @ModifyReturnValue(method = "parseForSuggestions", at = @At("RETURN"), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;suggest(Ljava/util/stream/Stream;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;")))
  private CompletableFuture<Suggestions> combineSuggestedValues(CompletableFuture<Suggestions> original, @Share(SHARE_NAME) LocalRef<List<CompletableFuture<Suggestions>>> share, @Local(argsOnly = true) SuggestionsBuilder builder) {
    final List<CompletableFuture<Suggestions>> extraSuggestions = share.get();
    if (extraSuggestions != null) {
      extraSuggestions.add(original);
      return ParseContext.combineMultipleSuggestions(builder, extraSuggestions);
    }
    return original;
  }
}
