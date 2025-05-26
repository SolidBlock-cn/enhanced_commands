package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record EntityNbtDataArgument(EntitySelector entitySelector) implements NbtSourceArgument<Entity>, NbtTargetArgument<Entity> {
  public EntityNbtData getEntityNbtData(ServerCommandSource source) throws CommandSyntaxException {
    return new EntityNbtData(entitySelector.getEntity(source));
  }

  @Override
  public EntityNbtData getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getEntityNbtData(source);
  }

  @Override
  public EntityNbtData getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getEntityNbtData(source);
  }

  public static EntityNbtDataArgument handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parseContext.reader());
    final EntitySelector selector = parseContext.parseAndSuggestArgument(EntityArgumentType.entity());
    return new EntityNbtDataArgument(selector);
  }
}
