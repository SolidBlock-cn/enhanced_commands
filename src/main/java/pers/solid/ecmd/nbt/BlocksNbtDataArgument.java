package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record BlocksNbtDataArgument(RegionArgument regionArgument, @Nullable BlockPredicateArgument blockPredicateArgument) implements NbtSourceArgument<BlockEntity>, NbtTargetArgument<BlockEntity> {
  public BlocksNbtDataArgument(RegionArgument regionArgument) {
    this(regionArgument, null);
  }

  public NbtTarget<BlockEntity> getBlockNbtData(ServerCommandSource source) throws CommandSyntaxException {
    final Region region = regionArgument.toAbsoluteRegion(source);
    final BlockPredicate blockPredicate = blockPredicateArgument == null ? null : blockPredicateArgument.apply(source);
    return new BlocksNbtData(region, blockPredicate);
  }

  @Override
  public NbtTarget<BlockEntity> getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return getBlockNbtData(source);
  }

  @Override
  public NbtTarget<BlockEntity> getNbtTarget(ServerCommandSource source) throws CommandSyntaxException {
    return getBlockNbtData(source);
  }

  public static BlocksNbtDataArgument handle(ParseContext<?> parseContext) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parseContext.reader());
    final RegionArgument regionArgument = RegionArgument.parse(parseContext);
    parseContext.clearSuggestion();
    return new BlocksNbtDataArgument(regionArgument);
  }
}
