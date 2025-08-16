package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockParser;
import pers.solid.ecmd.argument.SimpleBlockPredicateParser;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see TagBlockPredicate#properties
 */
public record PropertiesNamesBlockPredicate(@NotNull List<PropertyNamePredicate> predicates) implements BlockPredicate {
  public static final MapCodec<PropertiesNamesBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(PropertiesNamesBlockPredicate::new, PropertyNamePredicate.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(PropertiesNamesBlockPredicate::predicates)));

  @Override
  public @NotNull String asString() {
    return predicates.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(",", "[", "]"));
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    for (PropertyNamePredicate propertyNamePredicate : predicates) {
      if (!propertyNamePredicate.test(blockState))
        return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    boolean successes = true;
    List<TestResult> attachments = new ArrayList<>();
    final BlockPos blockPos = cachedBlockPosition.getBlockPos();
    for (PropertyNamePredicate propertyNamePredicate : predicates) {
      final TestResult testResult = propertyNamePredicate.testAndDescribe(blockState, blockPos);
      attachments.add(testResult);
      if (!testResult.successes()) {
        successes = false;
      }
    }
    if (attachments.size() == 1) {
      return attachments.get(0);
    } else if (successes) {
      return TestResult.of(true, Text.translatable("enhanced_commands.block_predicate.property_names.pass", TextUtil.wrapVector(blockPos)), attachments);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.block_predicate.property_names.fail", TextUtil.wrapVector(blockPos)), attachments);
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.PROPERTY_NAMES;
  }

  public enum Type implements BlockPredicateType<PropertiesNamesBlockPredicate>, Parser<PropertiesNamesBlockPredicate> {
    PROPERTY_NAMES_TYPE;

    @Override
    public @NotNull MapCodec<PropertiesNamesBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable PropertiesNamesBlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("[", SimpleBlockParser.START_OF_PROPERTIES, suggestionsBuilder).buildFuture());
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == '[') {
        final SimpleBlockPredicateParser<?> suggestedParser = new SimpleBlockPredicateParser<>(parseContext);
        suggestedParser.parsePropertyNames();
        return new PropertiesNamesBlockPredicate(suggestedParser.propertyNamePredicates);
      } else {
        return null;
      }
    }
  }
}
