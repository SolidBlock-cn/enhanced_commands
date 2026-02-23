package pers.solid.ecmd.argument;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import pers.solid.ecmd.predicate.entity.EntityPredicate;
import pers.solid.ecmd.predicate.entity.EntitySelectors;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record EntityPredicateArgumentType(CommandBuildContext registryAccess) implements ArgumentType<EntityPredicate> {
  private static final List<String> EXAMPLES = List.of("@a", "SolidBlock", "0123", "@r", "@e[distance=..5]", "[m=c]", "[gamemode=creative]");

  public static EntityPredicateArgumentType entityPredicate(CommandBuildContext registryAccess) {
    return new EntityPredicateArgumentType(registryAccess);
  }

  public static EntityPredicate getEntityPredicate(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, EntityPredicate.class);
  }

  @Override
  public EntityPredicate parse(StringReader reader) throws CommandSyntaxException {
    // 考虑命令主要是由管理员执行的，所以使用允许使用实体选择器。
    final EntitySelectorParser entitySelectorReader = new EntitySelectorParser(reader, true);
    return EntityPredicate.parse(entitySelectorReader);
  }

  @Override
  public <S> EntityPredicate parse(StringReader reader, S source) throws CommandSyntaxException {
    if (EntitySelectorParser.allowSelectors(source)) {
      return parse(reader);
    } else {
      final EntitySelectorParser entitySelectorReader = new EntitySelectorParser(reader, false);
      return EntityPredicate.simplifiedBySelector(entitySelectorReader.parse());
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    if (context.getSource() instanceof SharedSuggestionProvider commandSource) {
      StringReader stringReader = new StringReader(builder.getInput());
      stringReader.setCursor(builder.getStart());
      EntitySelectorParser entitySelectorReader = new EntitySelectorParser(stringReader, commandSource.hasPermission(2));
      entitySelectorReader.extension$ec().context = context;

      try {
        EntitySelectors.readOmittibleEntitySelector(entitySelectorReader);
      } catch (CommandSyntaxException ignored) {
      }

      return entitySelectorReader.fillSuggestions(builder, builder1 -> {
        Collection<String> collection = commandSource.getOnlinePlayerNames();
        Iterable<String> iterable = Iterables.concat(collection, commandSource.getSelectedEntities());
        SharedSuggestionProvider.suggest(iterable, builder1);
        if (builder1.getRemaining().isEmpty()) {
          builder1.suggest("[");
        }
      });
    } else {
      return Suggestions.empty();
    }
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
