package pers.solid.ecmd.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collections;
import java.util.List;

public record EntitiesNbtDataArgument(EntitySelector entitySelector) implements NbtSourceArgument<Entity>, NbtTargetArgument<Entity> {
  public NbtTarget<Entity> getEntitiesNbtData(ServerCommandSource source) throws CommandSyntaxException {
    final List<? extends Entity> entities = entitySelector.getEntities(source);
    return new EntitiesNbtData(Collections.unmodifiableList(entities));
  }

  @Override
  public NbtTarget<Entity> getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getEntitiesNbtData(source);
  }

  @Override
  public NbtTarget<Entity> getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getEntitiesNbtData(source);
  }

  public static EntitiesNbtDataArgument handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    ParsingUtil.expectAndSkipWhitespace(reader);
    final EntitySelector selector = parseContext.parseAndSuggestArgument(EntityArgumentType.entities());
    if (reader.canRead()) {
      parseContext.clearSuggestion();
    }
    return new EntitiesNbtDataArgument(selector);
  }
}
