package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.math.Checkerboard;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.Collections;

public record CheckerboardBlockPredicate(@NotNull WeightedList<BlockPredicate> predicates, @NotNull Vec3d floor, @NotNull Vec3d scale, @NotNull Vec3d offset) implements BlockPredicate, Checkerboard<BlockPredicate> {
  public static final MapCodec<CheckerboardBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      WeightedList.createMapCodec(BlockPredicate.CODEC).fieldOf("predicates").forGetter(CheckerboardBlockPredicate::predicates),
      Vec3d.CODEC.optionalFieldOf("floor", Vec3d.ZERO).forGetter(CheckerboardBlockPredicate::floor),
      Vec3d.CODEC.optionalFieldOf("scale", UNIT).forGetter(CheckerboardBlockPredicate::scale),
      Vec3d.CODEC.optionalFieldOf("offset", Vec3d.ZERO).forGetter(CheckerboardBlockPredicate::offset)
  ).apply(i, CheckerboardBlockPredicate::new));

  public CheckerboardBlockPredicate(@NotNull WeightedList<BlockPredicate> predicates) {
    this(predicates, Vec3d.ZERO, UNIT, Vec3d.ZERO);
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final BlockPredicate entry = getEntry(predicates, cachedBlockPosition.getBlockPos());
    if (entry == null) return false;
    return entry.test(cachedBlockPosition, context);
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final BlockPredicate entry = getEntry(predicates, cachedBlockPosition.getBlockPos());
    if (entry == null) return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.checkerboard.fail_no_checkerboard"));
    final TestResult testResult = entry.testAndDescribe(cachedBlockPosition, context);
    final MutableText wrapVector = TextUtil.wrapVector(cachedBlockPosition.getBlockPos());
    return testResult.successes() ? TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.checkerboard.pass", wrapVector), Collections.singletonList(testResult)) : TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.checkerboard.fail", wrapVector), Collections.singletonList(testResult));
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
    protected CheckerboardBlockPredicate getParseResult(Vec3d floor, Vec3d scale, Vec3d offset) {
      return new CheckerboardBlockPredicate(weightedList, floor, scale, offset);
    }

    @Override
    protected BlockPredicate parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
      return BlockPredicate.parse(parseContext);
    }
  }
}
