package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.SuggestedParser;
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

  public static EntityNbtDataArgument handle(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final EntitySelector selector = parser.parseAndSuggestArgument(EntityArgumentType.entity());
    return new EntityNbtDataArgument(selector);
  }
}
