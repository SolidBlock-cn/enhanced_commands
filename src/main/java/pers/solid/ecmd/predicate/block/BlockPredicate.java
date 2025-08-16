package pers.solid.ecmd.predicate.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.argument.SimpleBlockParser;
import pers.solid.ecmd.argument.SimpleBlockPredicateParser;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Collections;
import java.util.List;

public interface BlockPredicate extends ExpressionConvertible {
  Codec<BlockPredicate> MAP_CODEC = BlockPredicateType.REGISTRY.getCodec().dispatch(BlockPredicate::getType, BlockPredicateType::getCodec);
  Codec<BlockPredicate> CODEC = CodecUtil.combined(Registries.BLOCK.getCodec().xmap(block -> new SimpleBlockPredicate(block, ImmutableList.of()), SimpleBlockPredicate::block), MAP_CODEC, blockPredicate -> blockPredicate instanceof SimpleBlockPredicate s && s.properties().isEmpty() ? s : null);
  RegistryKey<Registry<BlockPredicate>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("block_predicate"));

  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.argument.block_predicate.cannot_parse"));
  Text INTERSECT_TOOLTIP = Text.translatable("enhanced_commands.block_predicate.all.symbol_tooltip");
  Text UNION_TOOLTIP = Text.translatable("enhanced_commands.block_predicate.any.symbol_tooltip");

  static TestResult successResult(BlockPos blockPos) {
    return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.pass", TextUtil.wrapVector(blockPos)));
  }

  static TestResult failResult(BlockPos blockPos) {
    return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.fail", TextUtil.wrapVector(blockPos)));
  }

  static TestResult successOrFail(boolean successes, BlockPos blockPos) {
    return successes ? successResult(blockPos) : failResult(blockPos);
  }

  static @NotNull BlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return parseUnion(parseContext);
  }

  static @NotNull BlockPredicate parseUnion(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseIntersect(parseContext), AnyBlockPredicate::new, "|", UNION_TOOLTIP, parseContext);
  }

  static @NotNull BlockPredicate parseIntersect(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseCombination(parseContext), AllBlockPredicate::new, "&", INTERSECT_TOOLTIP, parseContext);
  }

  static @NotNull <S> BlockPredicate parseCombination(ParseContext<S> parseContext) throws CommandSyntaxException {
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
        suggestionsBuilder.suggest("{", NbtPredicateParser.START_OF_COMPOUND);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == '{') {
      // 尝试读取 NBT
      nbtPredicate = new NbtPredicateParser<>(parseContext).parseCompound(false, false);
    } else nbtPredicate = null;
    if (propertyNamePredicates != null || nbtPredicate != null) {
      return new PropertiesNbtCombinationBlockPredicate(parseUnit, propertyNamePredicates == null ? null : new PropertiesNamesBlockPredicate(propertyNamePredicates), nbtPredicate == null ? null : new NbtBlockPredicate(nbtPredicate));
    }
    return parseUnit;
  }

  @NotNull
  static BlockPredicate parseUnit(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();
    // 刻意将 simple 调整到最后面
    for (Parser<BlockPredicate> argumentParser : Iterables.concat(BlockPredicateTypes.PARSERS, Collections.singleton(SimpleBlockPredicate.Type.SIMPLE_TYPE))) {
      reader.setCursor(cursorOnStart);
      final BlockPredicate parse = argumentParser.parse(parseContext);
      if (parse != null) {
        return parse;
      }
    }
    reader.setCursor(cursorOnStart);
    throw CANNOT_PARSE.createWithContext(reader);
  }

  boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context);

  default TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final boolean test = test(cachedBlockPosition, context);
    return successOrFail(test, cachedBlockPosition.getBlockPos());
  }

  @NotNull
  BlockPredicateType<?> getType();
}
