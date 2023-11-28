package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.Parser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

public enum ActiveRegionType implements RegionType<Region>, Parser<RegionArgument> {
  TYPE;

  @Override
  public RegionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {
    parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("$", Text.translatable("enhanced_commands.argument.region.active_region"), suggestionsBuilder));
    if (parser.reader.canRead() && parser.reader.peek() == '$') {
      parser.reader.skip();
      parser.suggestionProviders.clear();
      return source -> {
        try {
          return ((ServerPlayerEntityExtension) source.getPlayerOrThrow()).ec$getOrEvaluateActiveRegionOrThrow();
        } catch (CommandSyntaxException e) {
          throw new CommandException(Texts.toText(e.getRawMessage()));
        }
      };
    } else {
      return null;
    }
  }

  @Override
  public @NotNull Region fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
    throw new UnsupportedOperationException("Region NBT cannot hold this type of region");
  }
}
