package pers.solid.ecmd.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.EntitySelectorReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Mixin(EntitySelectorReader.class)
public interface EntitySelectorReaderAccessor {
  @Invoker
  void callReadArguments() throws CommandSyntaxException;

  @Invoker
  void callBuildPredicate();

  @Accessor
  BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> getSuggestionProvider();

  @Invoker
  CompletableFuture<Suggestions> callSuggestEndNext(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer);
}
