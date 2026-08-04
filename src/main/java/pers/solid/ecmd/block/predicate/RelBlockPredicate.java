package pers.solid.ecmd.block.predicate;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.Vec3iProvider;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record RelBlockPredicate(Vec3iProvider relPos, BlockPredicate predicate) implements BlockPredicate {

  public static final MapCodec<RelBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(RelBlockPredicate::new, Vec3iProvider.CODEC.fieldOf("rel_pos").forGetter(RelBlockPredicate::relPos), BlockPredicate.CODEC.fieldOf("predicate").forGetter(RelBlockPredicate::predicate)));

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockPos pos = blockInWorld.getPos().offset(relPos.toActualVector(executionContext.positionProvider));
    return predicate.test(new BlockInWorld(blockInWorld.getLevel(), pos, false), executionContext);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final Vec3i vector = relPos.toActualVector(executionContext.positionProvider);
    final BlockPos pos = blockInWorld.getPos().offset(vector);
    final TestResult testResult = predicate.testAndDescribe(new BlockInWorld(blockInWorld.getLevel(), pos, false), executionContext);
    return TestResult.of(testResult.successes(), Component.translatable("enhanced_commands.block_predicate.rel." + (testResult.successes() ? "pass" : "fail"), TextUtil.wrapVector(vector)), List.of(testResult));
  }

  @Override
  public BlockPredicateType<RelBlockPredicate> getType() {
    return BlockPredicateTypes.REL;
  }

  @Override
  public String expressAsString() {
    return "rel(%s, %s)".formatted(relPos.expressAsString(), predicate.expressAsString());
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(predicate);
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<RelBlockPredicate> {
    private @Nullable Vec3iProvider relPos;
    private @Nullable BlockPredicate blockPredicate;

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
        relPos = Vec3iProvider.parse(parseContext);
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicate.parse(parseContext);
      }
    }
  }
}
