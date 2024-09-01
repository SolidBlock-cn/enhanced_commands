package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.iterator.IterateUtils;

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
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockPredicate entry = getEntry(predicates, cachedBlockPosition.getBlockPos());
    if (entry == null) return false;
    return entry.test(cachedBlockPosition);
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final BlockPredicate entry = getEntry(predicates, cachedBlockPosition.getBlockPos());
    if (entry == null) return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.checkerboard.fail_no_checkerboard"));
    final TestResult testResult = entry.testAndDescribe(cachedBlockPosition);
    final MutableText wrapVector = TextUtil.wrapVector(cachedBlockPosition.getBlockPos());
    return testResult.successes() ? TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.checkerboard.pass", wrapVector), Collections.singletonList(testResult)) : TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.checkerboard.fail", wrapVector), Collections.singletonList(testResult));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
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


  public static class Parser extends CheckerboardParser<BlockPredicateArgument> {
    @Override
    protected BlockPredicateArgument getParseResult(Vec3d floor, Vec3d scale, Vec3d offset) {
      if (weighted) {
        return source -> new CheckerboardBlockPredicate(new WeightedList.Weighted<>(IterateUtils.transformFailableImmutableList(pairs, pair -> ObjectDoublePair.of(pair.left().apply(source), pair.rightDouble()))), floor, scale, offset);
      } else {
        return source -> new CheckerboardBlockPredicate(new WeightedList.Uniform<>(IterateUtils.transformFailableImmutableList(pairs, pair -> pair.left().apply(source))), floor, scale, offset);
      }
    }

    @Override
    protected BlockPredicateArgument parseElement(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly);
    }
  }
}
