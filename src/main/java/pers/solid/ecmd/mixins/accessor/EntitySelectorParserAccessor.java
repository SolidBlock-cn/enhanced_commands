package pers.solid.ecmd.mixins.accessor;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(EntitySelectorParser.class)
public interface EntitySelectorParserAccessor {
  @Accessor
  List<Predicate<Entity>> getPredicates();

  @Invoker
  void callParseOptions();

  @Invoker
  void callFinalizePredicates();

  @Accessor
  BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> getSuggestions();

  @Invoker
  CompletableFuture<Suggestions> callSuggestOptionsNextOrClose(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer);

  @Accessor
  void setUsesSelectors(boolean usesAt);
}
