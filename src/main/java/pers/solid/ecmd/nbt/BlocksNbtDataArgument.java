package pers.solid.ecmd.nbt;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.predicate.block.BlockPredicateContext;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public record BlocksNbtDataArgument(RegionArgument regionArgument, @Nullable BlockPredicateArgument blockPredicateArgument) implements NbtSourceArgument<BlockEntity>, NbtTargetArgument<BlockEntity> {
  public BlocksNbtDataArgument(RegionArgument regionArgument) {
    this(regionArgument, null);
  }

  public BlocksNbtData getBlockNbtData(ServerCommandSource source) throws CommandSyntaxException {
    final Region region = regionArgument.toAbsoluteRegion(source);
    final BlockPredicate blockPredicate = blockPredicateArgument == null ? null : blockPredicateArgument.apply(source);
    final ServerWorld world = source.getWorld();
    final ImmutableList<BlockEntity> blockEntities;
    final BlockBox blockBox = region.minContainingBlockBox();
    if (region.numberOfBlocksAffected() < 32L || blockBox == null) {
      blockEntities = region.stream().map(world::getBlockEntity).filter(Objects::nonNull).collect(ImmutableList.toImmutableList());
    } else {
      Set<WorldChunk> affectedChunks = new HashSet<>();
      for (BlockPos shrunkPos : BlockPos.iterate(MathHelper.floorDiv(blockBox.getMinX(), 16), 0, MathHelper.floorDiv(blockBox.getMinZ(), 16), MathHelper.floorDiv(blockBox.getMaxX(), 16), 0, MathHelper.floorDiv(blockBox.getMaxZ(), 16))) {
        final WorldChunk worldChunk = world.getChunkManager().getWorldChunk(shrunkPos.getX(), shrunkPos.getZ());
        if (worldChunk != null) affectedChunks.add(worldChunk);
      }
      Stream<Map.Entry<BlockPos, BlockEntity>> stream = affectedChunks.stream().flatMap(worldChunk -> worldChunk.getBlockEntities().entrySet().stream()).filter(entry -> region.contains(entry.getKey()));
      if (blockPredicate != null) {
        final BlockPredicateContext context = new BlockPredicateContext(world.getRandom(), null);
        stream = stream.filter(entry -> blockPredicate.test(new CachedBlockPosition(world, entry.getKey(), false), context));
      }
      blockEntities = stream.map(Map.Entry::getValue).collect(ImmutableList.toImmutableList());
    }
    return new BlocksNbtData(blockEntities);
  }

  @Override
  public BlocksNbtData getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getBlockNbtData(source);
  }

  @Override
  public BlocksNbtData getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getBlockNbtData(source);
  }

  public static BlocksNbtDataArgument handle(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final RegionArgument regionArgument = RegionArgument.parse(registryAccess, parser, suggestionsOnly);
    parser.clearSuggestion();
    return new BlocksNbtDataArgument(regionArgument);
  }
}
