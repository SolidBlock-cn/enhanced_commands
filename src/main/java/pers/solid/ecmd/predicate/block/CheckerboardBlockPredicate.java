package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collections;

public record CheckerboardBlockPredicate(@NotNull WeightedList<BlockPredicate> predicates, @NotNull Vec3 floor, @NotNull Vec3 scale, @NotNull Vec3 offset) implements BlockPredicate, Checkerboard<BlockPredicate> {
  public static final MapCodec<CheckerboardBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      WeightedList.createMapCodec(BlockPredicate.CODEC).fieldOf("predicates").forGetter(CheckerboardBlockPredicate::predicates),
      Vec3.CODEC.optionalFieldOf("floor", Vec3.ZERO).forGetter(CheckerboardBlockPredicate::floor),
      Vec3.CODEC.optionalFieldOf("scale", UNIT).forGetter(CheckerboardBlockPredicate::scale),
      Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(CheckerboardBlockPredicate::offset)
  ).apply(i, CheckerboardBlockPredicate::new));

  public CheckerboardBlockPredicate(@NotNull WeightedList<BlockPredicate> predicates) {
    this(predicates, Vec3.ZERO, UNIT, Vec3.ZERO);
  }

  @Override
  public boolean test(BlockInWorld cachedBlockPosition, ExecutionContext context) {
    final BlockPredicate entry = getEntry(predicates, cachedBlockPosition.getPos());
    if (entry == null) return false;
    return entry.test(cachedBlockPosition, context);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld cachedBlockPosition, ExecutionContext context) {
    final BlockPredicate entry = getEntry(predicates, cachedBlockPosition.getPos());
    if (entry == null) return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.checkerboard.fail_no_checkerboard"));
    final TestResult testResult = entry.testAndDescribe(cachedBlockPosition, context);
    final MutableComponent wrapVector = TextUtil.wrapVector(cachedBlockPosition.getPos());
    return testResult.successes() ? TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.checkerboard.pass", wrapVector), Collections.singletonList(testResult)) : TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.checkerboard.fail", wrapVector), Collections.singletonList(testResult));
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.CHECKERBOARD;
  }

  @Override
  public @NotNull String asString() {
    final StringBuilder sb = new StringBuilder("checkerboard(");
    sb.append(predicates.asString(ExpressionConvertible::asString));
    appendParameters(sb);
    return sb.append(")").toString();
  }

  public enum Type implements BlockPredicateType<CheckerboardBlockPredicate> {
    CHECKERBOARD_TYPE;

    @Override
    public @NotNull MapCodec<CheckerboardBlockPredicate> getCodec() {
      return CODEC;
    }
  }


  public static class Parser extends CheckerboardParser<BlockPredicate> {
    @Override
    protected CheckerboardBlockPredicate getParseResult(Vec3 floor, Vec3 scale, Vec3 offset) {
      return new CheckerboardBlockPredicate(weightedList, floor, scale, offset);
    }

    @Override
    protected BlockPredicate parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
      return BlockPredicate.parse(parseContext);
    }
  }
}
