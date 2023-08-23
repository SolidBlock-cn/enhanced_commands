package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SimpleBlockSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.StringRepresentablePredicate;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.NbtConvertible;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see TagBlockPredicate#propertyNamePredicates
 */
public record PropertyNamesPredicate(Collection<PropertyNamePredicate> propertyNamePredicates) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return "[" + propertyNamePredicates.stream().map(StringRepresentablePredicate::asString).collect(Collectors.joining(",")) + "]";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    for (PropertyNamePredicate propertyNamePredicate : propertyNamePredicates) {
      if (!propertyNamePredicate.test(blockState))
        return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    boolean successes = true;
    ImmutableList.Builder<Text> messages = new ImmutableList.Builder<>();
    for (PropertyNamePredicate propertyNamePredicate : propertyNamePredicates) {
      final TestResult testResult = propertyNamePredicate.testAndDescribe(blockState, cachedBlockPosition.getBlockPos());
      if (!testResult.successes()) {
        messages.addAll(testResult.descriptions());
        successes = false;
      }
    }
    return new TestResult(successes, messages.build());
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.PROPERTY_NAMES;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    final NbtList nbtList = new NbtList();
    nbtCompound.put("properties", nbtList);
    nbtList.addAll(Collections2.transform(propertyNamePredicates, NbtConvertible::createNbt));
  }

  public enum Type implements BlockPredicateType<PropertyNamesPredicate> {
    PROPERTY_NAMES_TYPE;

    @Override
    public @NotNull PropertyNamesPredicate fromNbt(@NotNull NbtCompound nbtCompound) {
      final List<PropertyNamePredicate> predicates = nbtCompound.getList("predicates", NbtElement.COMPOUND_TYPE)
          .stream()
          .map(nbtElement -> PropertyNamePredicate.fromNbt((NbtCompound) nbtElement))
          .toList();
      return new PropertyNamesPredicate(predicates);
    }

    @Override
    public @Nullable PropertyNamesPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.suggestions.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("[", SimpleBlockSuggestedParser.START_OF_PROPERTIES, suggestionsBuilder));
      if (parser.reader.canRead() && parser.reader.peek() == '[') {
        final SimpleBlockPredicateSuggestedParser suggestedParser = new SimpleBlockPredicateSuggestedParser(commandRegistryAccess, parser);
        suggestedParser.parsePropertyNames();
        return new PropertyNamesPredicate(suggestedParser.propertyNamePredicates);
      } else {
        return null;
      }
    }
  }
}
