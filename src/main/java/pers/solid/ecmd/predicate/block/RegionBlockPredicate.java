package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.FunctionLikeParser;
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
    nbtCompound.putString("region", region.asString());
  }

  public enum Type implements BlockPredicateType<RegionBlockPredicate> {
    REGION_TYPE;

    @Override
    public @NotNull RegionBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable BlockPredicateArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  private static final class Parser implements FunctionLikeParser<BlockPredicateArgument> {
    private RegionArgument<?> regionArgument;

    @Override
    public @NotNull String functionName() {
      return "region";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.block_predicate.region");
    }

    @Override
    public BlockPredicateArgument getParseResult(SuggestedParser parser) {
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
