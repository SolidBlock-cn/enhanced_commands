package pers.solid.ecmd.argument;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.mixin.EntitySelectorReaderAccessor;
import pers.solid.ecmd.predicate.entity.EntityPredicate;
import pers.solid.ecmd.predicate.entity.EntityPredicateArgument;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record EntityPredicateArgumentType(CommandRegistryAccess commandRegistryAccess) implements ArgumentType<EntityPredicateArgument> {
  private static final List<String> EXAMPLES = List.of("@a", "SolidBlock", "0123", "@r", "@e[distance=..5]", "[m=c]", "[gamemode=creative]");

  public static EntityPredicateArgumentType entityPredicate(CommandRegistryAccess commandRegistryAccess) {
    return new EntityPredicateArgumentType(commandRegistryAccess);
  }

  public static EntityPredicate getEntityPredicate(CommandContext<ServerCommandSource> context, String name) {
    return context.getArgument(name, EntityPredicateArgument.class).apply(context.getSource());
  }

  @Override
  public EntityPredicateArgument parse(StringReader reader) throws CommandSyntaxException {
    final EntitySelectorReader entitySelectorReader = new EntitySelectorReader(reader);
    if (reader.canRead() && reader.peek() == '[') {
      reader.skip();
      ((EntitySelectorReaderAccessor) entitySelectorReader).callReadArguments();
      ((EntitySelectorReaderAccessor) entitySelectorReader).callBuildPredicate();
      return EntityPredicateArgument.of(entitySelectorReader.build());
    } else {
      return EntityPredicateArgument.of(entitySelectorReader.read());
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    if (context.getSource() instanceof CommandSource commandSource) {
      StringReader stringReader = new StringReader(builder.getInput());
      stringReader.setCursor(builder.getStart());
      EntitySelectorReader entitySelectorReader = new EntitySelectorReader(stringReader, commandSource.hasPermissionLevel(2));
      final var accessor = (EntitySelectorReaderAccessor) entitySelectorReader;

      try {
        if (stringReader.canRead() && stringReader.peek() == '[') {
          stringReader.skip();
          accessor.callReadArguments();
        } else {
          entitySelectorReader.read();
        }
      } catch (CommandSyntaxException ignored) {
      }

      return entitySelectorReader.listSuggestions(builder, builder1 -> {
        Collection<String> collection = commandSource.getPlayerNames();
        Iterable<String> iterable = Iterables.concat(collection, commandSource.getEntitySuggestions());
        CommandSource.suggestMatching(iterable, builder1);
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
