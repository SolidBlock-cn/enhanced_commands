package pers.solid.ecmd.block.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.property.predicate.PropertyPredicate;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record SimpleBlockPredicate(Block block, List<PropertyPredicate<?>> properties) implements BlockPredicate, DoesNotRequireValidation {
  public static final Codec<SimpleBlockPredicate> STRING_BASED_CODEC = BuiltInRegistries.BLOCK.byNameCodec().flatComapMap(block -> new SimpleBlockPredicate(block, ImmutableList.of()), simpleBlockPredicate -> simpleBlockPredicate.properties.isEmpty() ? DataResult.success(simpleBlockPredicate.block) : DataResult.error(() -> "cannot serialize predicate with properties to strings"));

  public static final MapCodec<SimpleBlockPredicate> CODEC = BuiltInRegistries.BLOCK.byNameCodec().dispatchMap("block", SimpleBlockPredicate::block, block -> RecordCodecBuilder.mapCodec(i -> i.ap(properties -> new SimpleBlockPredicate(block, properties), CodecUtil.optionalField("properties", PropertyPredicate.getCodec(block).listOf(), ImmutableList.of()).forGetter(SimpleBlockPredicate::properties))));

  public SimpleBlockPredicate(Block block) {
    this(block, Collections.emptyList());
  }


  @Override
  public String expressAsString() {
    final String id = DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(BuiltInRegistries.BLOCK.getKey(block));
    return properties.isEmpty() ? id : id + properties.stream().map(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ", "[", "]"));
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    if (!blockInWorld.getState().is(block))
      return false;
    for (PropertyPredicate<?> propertyPredicate : properties) {
      if (!propertyPredicate.test(blockInWorld.getState()))
        return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    boolean matches = true;
    final BlockState blockState = blockInWorld.getState();
    final List<Component> messages = new ArrayList<>();
    final BlockPos blockPos = blockInWorld.getPos();
    final MutableComponent posText = TextUtil.wrapVector(blockPos);
    final MutableComponent actualText = blockState.getBlock().getName().withStyle(Styles.ACTUAL);
    if (!blockState.is(block)) {
      final MutableComponent expectedText = block.getName().withStyle(Styles.EXPECTED);
      messages.add(Component.translatable("enhanced_commands.block_predicate.simple.not_the_block", posText, actualText, expectedText).withStyle(Styles.FALSE));
      matches = false;
    } else {
      messages.add(Component.translatable("enhanced_commands.block_predicate.simple.is_the_block", posText, actualText).withStyle(Styles.TRUE));
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
  public BlockPredicateType<SimpleBlockPredicate> getType() {
    return BlockPredicateTypes.SIMPLE;
  }

  public enum SimpleParser implements Parser<BlockPredicate> {
    INSTANCE;

    @Override
    public BlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      SimpleBlockPredicateParser<?> parser = new SimpleBlockPredicateParser<>(parseContext);
      parser.parseBlockId();
      parser.parseProperties();
      Objects.requireNonNull(parser.block, "block");
      return new SimpleBlockPredicate(parser.block, parser.propertyPredicates);
    }
  }
}
