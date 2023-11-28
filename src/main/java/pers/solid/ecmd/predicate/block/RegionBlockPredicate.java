package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.TextUtil;

public record RegionBlockPredicate(Region region) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return "region(" + region.asString() + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return region.contains(cachedBlockPosition.getBlockPos());
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final BlockPos blockPos = cachedBlockPosition.getBlockPos();
    final boolean contains = region.contains(blockPos);
    return TestResult.of(contains, Text.translatable("enhanced_commands.argument.block_predicate.region." + (contains ? "pass" : "fail"), TextUtil.wrapVector(blockPos), TextUtil.literal(region).styled(TextUtil.STYLE_FOR_ACTUAL)));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.REGION;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("region", region.createNbt());
  }

  public enum Type implements BlockPredicateType<RegionBlockPredicate> {
    REGION_TYPE;

    @Override
    public @NotNull RegionBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new RegionBlockPredicate(Region.fromNbt(nbtCompound.getCompound("region"), world));
    }
  }

  public static final class Parser implements FunctionParamsParser<BlockPredicateArgument> {
    private RegionArgument regionArgument;

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return serverCommandSource -> new RegionBlockPredicate(regionArgument.toAbsoluteRegion(serverCommandSource));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      regionArgument = RegionArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
    }

    // @formatter:off
    @Override public int minParamsCount() { return 1; }
    @Override public int maxParamsCount() { return 1; }
    // @formatter:on
  }
}
