package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SimpleBlockSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

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
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    for (PropertyNamePredicate propertyNamePredicate : predicates) {
      if (!propertyNamePredicate.test(blockState))
        return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
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
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.PROPERTY_NAMES;
  }

  public enum Type implements BlockPredicateType<PropertiesNamesBlockPredicate>, Parser<BlockPredicateArgument> {
    PROPERTY_NAMES_TYPE;

    @Override
    public @NotNull MapCodec<PropertiesNamesBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable PropertiesNamesBlockPredicate parse(CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("[", SimpleBlockSuggestedParser.START_OF_PROPERTIES, suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '[') {
        final SimpleBlockPredicateSuggestedParser suggestedParser = new SimpleBlockPredicateSuggestedParser(registryAccess, parser);
        suggestedParser.parsePropertyNames();
        return new PropertiesNamesBlockPredicate(suggestedParser.propertyNamePredicates);
      } else {
        return null;
      }
    }
  }
}
