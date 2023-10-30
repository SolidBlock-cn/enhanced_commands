package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

public enum ActiveRegionType implements RegionType<Region> {
  TYPE;

  @Override
  public @Nullable RegionArgument<?> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("$", Text.translatable("enhancedCommands.argument.region.active_region"), suggestionsBuilder));
    if (parser.reader.canRead() && parser.reader.peek() == '$') {
      parser.reader.skip();
      parser.suggestionProviders.clear();
      return source -> {
        try {
          return ((ServerPlayerEntityExtension) source.getPlayerOrThrow()).ec$getOrEvaluateActiveRegionOrThrow(source);
        } catch (CommandSyntaxException e) {
          throw new CommandException((Text) e.getRawMessage());
        }
      };
    } else {
      return null;
    }
  }
}
