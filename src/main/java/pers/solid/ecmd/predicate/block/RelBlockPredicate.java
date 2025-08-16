package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.Vec3iArgument;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record RelBlockPredicate(@NotNull Vec3iArgument relPos, @NotNull BlockPredicate predicate) implements BlockPredicate {

  public static final MapCodec<RelBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(RelBlockPredicate::new, Vec3iArgument.CODEC.fieldOf("rel_pos").forGetter(RelBlockPredicate::relPos), BlockPredicate.CODEC.fieldOf("predicate").forGetter(RelBlockPredicate::predicate)));

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final BlockPos pos = cachedBlockPosition.getBlockPos().add(relPos.toActualVector(context.positionProvider));
    return predicate.test(new CachedBlockPosition(cachedBlockPosition.getWorld(), pos, false), context);
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final Vec3i vector = relPos.toActualVector(context.positionProvider);
    final BlockPos pos = cachedBlockPosition.getBlockPos().add(vector);
    final TestResult testResult = predicate.testAndDescribe(new CachedBlockPosition(cachedBlockPosition.getWorld(), pos, false), context);
    return TestResult.of(testResult.successes(), Text.translatable("enhanced_commands.block_predicate.rel." + (testResult.successes() ? "pass" : "fail"), TextUtil.wrapVector(vector)), List.of(testResult));
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.REL;
  }

  @Override
  public @NotNull String asString() {
    return "rel(%s, %s)".formatted(relPos.asString(), predicate.asString());
  }

  public enum Type implements BlockPredicateType<RelBlockPredicate> {
    REL_TYPE;

    @Override
    public @NotNull MapCodec<RelBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.SequentialParams<RelBlockPredicate> {
    private Vec3iArgument relPos;
    private BlockPredicate blockPredicate;

    @Override
    public RelBlockPredicate getParseResult(ParseContext<?> parseContext) {
      Preconditions.checkNotNull(relPos, "relPos (argument 1)");
      Preconditions.checkNotNull(blockPredicate, "predicate (argument 2)");
      return new RelBlockPredicate(relPos, blockPredicate);
    }

    @Override
    public int minSequentialParamsCount() {
      return 2;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        relPos = Vec3iArgument.parse(parseContext);
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicate.parse(parseContext);
      }
    }
  }
}
