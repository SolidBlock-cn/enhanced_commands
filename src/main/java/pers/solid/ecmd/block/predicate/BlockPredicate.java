package pers.solid.ecmd.block.predicate;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.SimpleBlockParser;
import pers.solid.ecmd.nbt.NbtParserShared;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.nbt.predicate.NbtPredicateParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.property.predicate.PropertyNamePredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public interface BlockPredicate extends ExpressionConvertible {
  MapCodec<BlockPredicate> MAP_CODEC = BlockPredicateType.CODEC.dispatchMap(BlockPredicate::getType, BlockPredicateType::codec);
  Codec<BlockPredicate> CODEC = Codec.lazyInitialized(() -> CodecUtil.combined(
      CodecUtil.combinedIdAndTag(SimpleBlockPredicate.STRING_BASED_CODEC, TagBlockPredicate.STRING_BASED_CODEC),
      MAP_CODEC.codec(),
      blockPredicate -> blockPredicate instanceof SimpleBlockPredicate s && s.properties().isEmpty() ? Either.left(s) : blockPredicate instanceof TagBlockPredicate t && t.properties().isEmpty() ? Either.right(t) : null,
      either -> either.map(Function.identity(), Function.identity())));
  ResourceKey<Registry<BlockPredicate>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("block_predicate"));

  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.argument.block_predicate.cannot_parse"));
  Component INTERSECT_TOOLTIP = Component.translatable("enhanced_commands.predicate.all.symbol_tooltip");
  Component UNION_TOOLTIP = Component.translatable("enhanced_commands.predicate.any.symbol_tooltip");

  static TestResult successResult(BlockPos blockPos) {
    return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.pass", TextUtil.wrapVector(blockPos)));
  }

  static TestResult failResult(BlockPos blockPos) {
    return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.fail", TextUtil.wrapVector(blockPos)));
  }

  static TestResult successOrFail(boolean successes, BlockPos blockPos) {
    return successes ? successResult(blockPos) : failResult(blockPos);
  }

  static BlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return parseUnion(parseContext);
  }

  static BlockPredicate parseUnion(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseIntersect(parseContext), AnyBlockPredicate::new, "|", UNION_TOOLTIP, parseContext);
  }

  static BlockPredicate parseIntersect(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseCombination(parseContext), AllBlockPredicate::new, "&", INTERSECT_TOOLTIP, parseContext);
  }

  static <S> BlockPredicate parseCombination(ParseContext<S> parseContext) throws CommandSyntaxException {
    final BlockPredicate parseUnit = parseUnit(parseContext);
    if (parseUnit instanceof NbtPredicate) {
      return parseUnit;
    }
    final StringReader reader = parseContext.reader();
    List<PropertyNamePredicate> propertyNamePredicates;
    if (!(parseUnit instanceof PropertiesNamesBlockPredicate) && reader.canRead(0) && reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parseContext.addSuggestion((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest("[", SimpleBlockParser.START_OF_PROPERTIES);
        }
        return suggestionsBuilder.buildFuture();
      });
      if (reader.canRead() && reader.peek() == '[') {
        final SimpleBlockPredicateParser<S> suggestedParser = new SimpleBlockPredicateParser<>(parseContext);
        suggestedParser.parsePropertyNames();
        propertyNamePredicates = suggestedParser.propertyNamePredicates;
      } else propertyNamePredicates = null;
    } else propertyNamePredicates = null;
    NbtPredicate nbtPredicate;
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("{", NbtParserShared.START_OF_COMPOUND);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == '{') {
      // 尝试读取 NBT
      nbtPredicate = NbtPredicateParser.parseCompound(parseContext, false);
    } else nbtPredicate = null;
    if (propertyNamePredicates != null || nbtPredicate != null) {
      return new PropertiesNbtCombinationBlockPredicate(parseUnit, propertyNamePredicates == null ? null : new PropertiesNamesBlockPredicate(propertyNamePredicates), nbtPredicate == null ? null : new NbtBlockPredicate(nbtPredicate));
    }
    return parseUnit;
  }

  static BlockPredicate parseUnit(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();
    // 刻意将 simple 调整到最后面
    for (Parser<? extends BlockPredicate> argumentParser : Iterables.concat(BlockPredicateParsing.PARSERS, Collections.singleton(SimpleBlockPredicate.SimpleParser.INSTANCE))) {
      reader.setCursor(cursorOnStart);
      final BlockPredicate parse = argumentParser.parse(parseContext);
      if (parse != null) {
        return parse;
      }
    }
    reader.setCursor(cursorOnStart);
    throw CANNOT_PARSE.createWithContext(reader);
  }

  boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext);

  default TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final boolean test = test(blockInWorld, executionContext);
    return successOrFail(test, blockInWorld.getPos());
  }

  BlockPredicateType<?> getType();
}
