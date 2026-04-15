package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionProvider;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Objects;

public record RegionBlockPredicate(RegionProvider<?> region) implements BlockPredicate {
  public static final MapCodec<RegionBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(RegionBlockPredicate::new, RegionProvider.CODEC.fieldOf("region").forGetter(RegionBlockPredicate::region)));

  @Override
  public String asString() {
    return "region(" + region.asString() + ")";
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    return Region.getCached(region, executionContext.positionProvider).contains(blockInWorld.getPos());
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockPos blockPos = blockInWorld.getPos();
    final boolean contains;
    contains = region.toAbsoluteRegion(executionContext.positionProvider).contains(blockPos);
    return TestResult.of(contains, Component.translatable("enhanced_commands.block_predicate.region." + (contains ? "pass" : "fail"), TextUtil.wrapVector(blockPos), TextUtil.literal(region).withStyle(Styles.ACTUAL)));
  }

  @Override
  public BlockPredicateType<RegionBlockPredicate> getType() {
    return BlockPredicateTypes.REGION;
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<RegionBlockPredicate> {
    private @Nullable RegionProvider<?> regionProvider;

    @Override
    public RegionBlockPredicate getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(regionProvider, "regionProvider");
      return new RegionBlockPredicate(regionProvider);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      regionProvider = RegionProvider.parse(parseContext);
    }

    // @formatter:off
    @Override public int minSequentialParamsCount() { return 1; }
    @Override public int maxSequentialParamsCount() { return 1; }
    // @formatter:on
  }
}
