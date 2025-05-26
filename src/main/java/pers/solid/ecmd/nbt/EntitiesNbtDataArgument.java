package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collections;
import java.util.List;

public record EntitiesNbtDataArgument(EntitySelector entitySelector) implements NbtSourceArgument<Entity>, NbtTargetArgument<Entity> {
  public NbtTarget<Entity> getEntitiesNbtData(ServerCommandSource source) throws CommandSyntaxException {
    final List<? extends Entity> entities = entitySelector.getEntities(source);
    if (entities.size() == 1) {
      return new EntityNbtData(entities.getFirst());
    } else {
      return new EntitiesNbtData(Collections.unmodifiableList(entities));
    }
  }

  @Override
  public NbtTarget<Entity> getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getEntitiesNbtData(source);
  }

  @Override
  public NbtTarget<Entity> getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getEntitiesNbtData(source);
  }

  public static EntitiesNbtDataArgument handle(SuggestedParser<?> parser) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final EntitySelector selector = parser.parseAndSuggestArgument(EntityArgumentType.entities());
    if (parser.reader.canRead()) {
      parser.clearSuggestion();
    }
    return new EntitiesNbtDataArgument(selector);
  }
}
