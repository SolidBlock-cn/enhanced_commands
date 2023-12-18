package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.Objects;

public record BlocksNbtDataArgument(RegionArgument regionArgument, NbtConcentrationType nbtConcentrationType) implements NbtSourceArgument, NbtTargetArgument {
  public BlocksNbtData getBlockNbtData(ServerCommandSource source) throws CommandSyntaxException {
    final Region region = regionArgument.toAbsoluteRegion(source);
    final ServerWorld world = source.getWorld();
    final ImmutableList<BlockEntity> blockEntities = region.stream().map(world::getBlockEntity).filter(Objects::nonNull).collect(ImmutableList.toImmutableList());
    return new BlocksNbtData(blockEntities, nbtConcentrationType, source.getWorld().getRandom());
  }

  @Override
  public BlocksNbtData getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getBlockNbtData(source);
  }

  @Override
  public BlocksNbtData getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getBlockNbtData(source);
  }

  public static BlocksNbtDataArgument handle(CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean requiresConcentration) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final RegionArgument regionArgument = RegionArgument.parse(registryAccess, parser, suggestionsOnly);
    parser.suggestionProviders.clear();
    if (requiresConcentration) {
      ParsingUtil.expectAndSkipWhitespace(parser.reader);
      final NbtConcentrationType nbtConcentrationType = parser.parseAndSuggestEnums(NbtConcentrationType.values(), NbtConcentrationType::getDisplayName, NbtConcentrationType.CODEC);
      parser.suggestionProviders.clear();
      return new BlocksNbtDataArgument(regionArgument, nbtConcentrationType);
    } else {
      return new BlocksNbtDataArgument(regionArgument, null);
    }
  }
}
