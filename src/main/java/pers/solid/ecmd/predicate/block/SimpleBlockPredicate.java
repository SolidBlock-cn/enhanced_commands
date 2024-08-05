package pers.solid.ecmd.predicate.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.property.PropertyPredicate;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record SimpleBlockPredicate(Block block, List<PropertyPredicate<?>> properties) implements BlockPredicate {
  public static final MapCodec<SimpleBlockPredicate> CODEC = Registries.BLOCK.getCodec().dispatchMap("block", SimpleBlockPredicate::block, block -> RecordCodecBuilder.mapCodec(i -> i.ap(properties -> new SimpleBlockPredicate(block, properties), CodecUtil.optionalField("properties", PropertyPredicate.getCodec(block).listOf(), ImmutableList.of()).forGetter(SimpleBlockPredicate::properties))));


  @Override
  public @NotNull String asString() {
    final String id = Registries.BLOCK.getId(block).toString();
    return properties.isEmpty() ? id : id + properties.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ", "[", "]"));
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    if (!cachedBlockPosition.getBlockState().isOf(block))
      return false;
    for (PropertyPredicate<?> propertyPredicate : properties) {
      if (!propertyPredicate.test(cachedBlockPosition.getBlockState()))
        return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    boolean matches = true;
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final List<Text> messages = new ArrayList<>();
    final BlockPos blockPos = cachedBlockPosition.getBlockPos();
    final MutableText posText = TextUtil.wrapVector(blockPos);
    final MutableText actualText = blockState.getBlock().getName().styled(Styles.ACTUAL);
    if (!blockState.isOf(block)) {
      final MutableText expectedText = block.getName().styled(Styles.EXPECTED);
      messages.add(Text.translatable("enhanced_commands.block_predicate.simple.not_the_block", posText, actualText, expectedText).styled(Styles.FALSE));
      matches = false;
    } else {
      messages.add(Text.translatable("enhanced_commands.block_predicate.simple.is_the_block", posText, actualText).styled(Styles.TRUE));
    }
    for (PropertyPredicate<?> propertyPredicate : properties) {
      final TestResult propertyResult = propertyPredicate.testAndDescribe(blockState, blockPos);
      messages.addAll(propertyResult.descriptions());
      if (!propertyResult.successes()) {
        matches = false;
      }
    }
    return new TestResult(matches, messages);
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.SIMPLE;
  }

  public enum Type implements BlockPredicateType<SimpleBlockPredicate>, Parser<BlockPredicateArgument> {
    SIMPLE_TYPE;

    @Override
    public @NotNull MapCodec<SimpleBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull BlockPredicate parse(CommandRegistryAccess registryAccess, SuggestedParser parser0, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      SimpleBlockPredicateSuggestedParser parser = new SimpleBlockPredicateSuggestedParser(registryAccess, parser0);
      parser.parseBlockId();
      parser.parseProperties();
      return new SimpleBlockPredicate(parser.block, parser.propertyPredicates);
    }
  }
}
