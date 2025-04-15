package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collections;

public record EntitiesNbtDataArgument(EntitySelector entitySelector, NbtConcentrationType nbtConcentrationType) implements NbtSourceArgument<Entity>, NbtTargetArgument<Entity> {
  public EntitiesNbtData getEntitiesNbtData(ServerCommandSource source) throws CommandSyntaxException {
    return new EntitiesNbtData(Collections.unmodifiableCollection(entitySelector.getEntities(source)), nbtConcentrationType, source.getWorld().getRandom());
  }

  @Override
  public EntitiesNbtData getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getEntitiesNbtData(source);
  }

  @Override
  public EntitiesNbtData getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getEntitiesNbtData(source);
  }

  public static EntitiesNbtDataArgument handle(SuggestedParser<?> parser, boolean hasConcentration) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final EntitySelector selector = parser.parseAndSuggestArgument(EntityArgumentType.entities());
    if (parser.reader.canRead()) {
      parser.clearSuggestion();
    }
    if (hasConcentration) {
      final int cursorBeforeWhite = parser.reader.getCursor();
      parser.reader.skipWhitespace();
      if (cursorBeforeWhite == parser.reader.getCursor()) {
        return new EntitiesNbtDataArgument(selector, NbtConcentrationType.ALL);
      }
      final NbtConcentrationType nbtConcentrationType = parser.parseAndSuggestEnums(NbtConcentrationType.values(), NbtConcentrationType::getDisplayName, NbtConcentrationType.CODEC);
      parser.clearSuggestion();
      return new EntitiesNbtDataArgument(selector, nbtConcentrationType);
    } else {
      return new EntitiesNbtDataArgument(selector, null);
    }
  }
}
