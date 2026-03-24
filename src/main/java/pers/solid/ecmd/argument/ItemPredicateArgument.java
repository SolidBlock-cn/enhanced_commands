package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.mixins.accessor.ItemPredicateArgumentAccessor;
import pers.solid.ecmd.predicate.item.ItemPredicate;
import pers.solid.ecmd.predicate.item.SimpleCombinationItemPredicate;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * @see net.minecraft.commands.arguments.item.ItemPredicateArgument
 */
public class ItemPredicateArgument implements ArgumentType<ItemPredicate> {
  private static final List<String> EXAMPLES = List.of("diamond", "#planks", "diamond_sword[enchantments={sharpness:1}]");

  private final net.minecraft.commands.arguments.item.ItemPredicateArgument forward;

  public ItemPredicateArgument(CommandBuildContext context) {
    forward = new net.minecraft.commands.arguments.item.ItemPredicateArgument(context);
  }

  public static ItemPredicateArgument itemPredicate(CommandBuildContext context) {
    return new ItemPredicateArgument(context);
  }

  public static ItemPredicate getItemPredicate(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ItemPredicate.class);
  }

  /**
   * @see pers.solid.ecmd.mixins.general.ItemPredicateArgumentMixin.ContextMixin#combine$enhanced_commands(List)
   */
  @Override
  public ItemPredicate parse(StringReader stringReader) throws CommandSyntaxException {
    final Grammar<List<Predicate<ItemStack>>> grammarWithContext = ((ItemPredicateArgumentAccessor) forward).getGrammarWithContext();
    final List<Predicate<ItemStack>> result = grammarWithContext.parseForCommands(stringReader);
    if (result.size() == 1) {
      return ItemPredicate.convertOrUnknown(result.get(0));
    } else {
      return SimpleCombinationItemPredicate.of(result.stream().map(ItemPredicate::convertOrUnknown).collect(ImmutableList.toImmutableList()));
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return forward.listSuggestions(context, builder);
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
