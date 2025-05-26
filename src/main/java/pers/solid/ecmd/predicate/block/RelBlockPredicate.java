package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.List;
import java.util.function.Function;

public record RelBlockPredicate(@NotNull Vec3i relPos, @NotNull BlockPredicate predicate) implements BlockPredicate {

  public static final MapCodec<RelBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(RelBlockPredicate::new, Vec3i.CODEC.fieldOf("rel_pos").forGetter(RelBlockPredicate::relPos), BlockPredicate.CODEC.fieldOf("predicate").forGetter(RelBlockPredicate::predicate)));

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context) {
    final BlockPos pos = cachedBlockPosition.getBlockPos().add(relPos);
    return predicate.test(new CachedBlockPosition(cachedBlockPosition.getWorld(), pos, false), context);
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, BlockPredicateContext context) {
    final BlockPos pos = cachedBlockPosition.getBlockPos().add(relPos);
    final TestResult testResult = predicate.testAndDescribe(new CachedBlockPosition(cachedBlockPosition.getWorld(), pos, false), context);
    return TestResult.of(testResult.successes(), Text.translatable("enhanced_commands.block_predicate.rel." + (testResult.successes() ? "pass" : "fail"), TextUtil.wrapVector(relPos)), List.of(testResult));
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.REL;
  }

  @Override
  public @NotNull String asString() {
    return "rel(%s %s %s, %s)".formatted(relPos.getX(), relPos.getY(), relPos.getZ(), predicate.asString());
  }

  public enum Type implements BlockPredicateType<RelBlockPredicate> {
    REL_TYPE;

    @Override
    public @NotNull MapCodec<RelBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionParamsParser<BlockPredicateArgument> {
    private Function<ServerCommandSource, Vec3i> relPos;
    private BlockPredicateArgument blockPredicate;

    @Override
    public BlockPredicateArgument getParseResult(ParseContext<?> parseContext) {
      Preconditions.checkNotNull(relPos, "relPos (argument 1)");
      Preconditions.checkNotNull(blockPredicate, "predicate (argument 2)");
      return source -> new RelBlockPredicate(relPos.apply(source), blockPredicate.apply(source));
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        relPos = parseContext.parser().parseAndSuggestVec3i();
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicateArgument.parse(parseContext);
      }
    }
  }
}
