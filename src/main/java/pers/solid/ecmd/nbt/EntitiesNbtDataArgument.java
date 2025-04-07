package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record EntitiesNbtDataArgument(EntitySelector entitySelector, NbtConcentrationType nbtConcentrationType) implements NbtSourceArgument, NbtTargetArgument {
  public EntitiesNbtData getEntitiesNbtData(ServerCommandSource source) throws CommandSyntaxException {
    return new EntitiesNbtData(entitySelector.getEntities(source), nbtConcentrationType, source.getWorld().getRandom());
  }

  @Override
  public EntitiesNbtData getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getEntitiesNbtData(source);
  }

  @Override
  public EntitiesNbtData getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getEntitiesNbtData(source);
  }

  public static EntitiesNbtDataArgument handle(SuggestedParser<?> suggestedParser, boolean requiresConcentration) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(suggestedParser.reader);
    final EntitySelector selector = suggestedParser.parseAndSuggestArgument(EntityArgumentType.entities());
    if (suggestedParser.reader.canRead()) {
      suggestedParser.clearSuggestion();
    }
    if (requiresConcentration) {
      NbtSource.expectConcentrationType(suggestedParser.reader);
      final NbtConcentrationType nbtConcentrationType = suggestedParser.parseAndSuggestEnums(NbtConcentrationType.values(), NbtConcentrationType::getDisplayName, NbtConcentrationType.CODEC);
      suggestedParser.clearSuggestion();
      return new EntitiesNbtDataArgument(selector, nbtConcentrationType);
    } else {
      return new EntitiesNbtDataArgument(selector, null);
    }
  }
}
