package pers.solid.mod.predicate.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import pers.solid.mod.EnhancedCommands;
import pers.solid.mod.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.mod.argument.SuggestedParser;
import pers.solid.mod.command.TestResult;
import pers.solid.mod.predicate.SerializablePredicate;
import pers.solid.mod.predicate.property.PropertyNameEntry;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @see BlockArgumentParser#parseTagId()
 */
public record TagBlockPredicate(@NotNull TagKey<Block> blockTag, @NotNull@UnmodifiableView Collection<PropertyNameEntry> propertyNameEntries) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    if (propertyNameEntries.isEmpty()) {
      return "#" + blockTag.id().toString();
    } else {
      return "#" + blockTag.id().toString() + "[" + propertyNameEntries.stream().map(SerializablePredicate::asString).collect(Collectors.joining(",")) + "]";
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final boolean inTag = blockState.isIn(blockTag);
    if (!inTag) {
      return false;
    }
    for (PropertyNameEntry propertyNameEntry : propertyNameEntries) {
      if (!propertyNameEntry.test(blockState)) return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final boolean inTag = blockState.isIn(blockTag);
    boolean successes = true;
    ImmutableList.Builder<Text> messages = new ImmutableList.Builder<>();
    if (!inTag) {
      successes = false;
      messages.add(Text.translatable("blockPredicate.not_in_the_tag", EnhancedCommands.wrapBlockPos(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_ACTUAL), Text.literal("#" + blockTag.id().toString()).styled(EnhancedCommands.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
    }
    for (PropertyNameEntry propertyNameEntry : propertyNameEntries) {
      final TestResult testResult = propertyNameEntry.testAndDescribe(blockState, cachedBlockPosition.getBlockPos());
      if (!testResult.successes()) {
        messages.addAll(testResult.descriptions());
        successes = false;
      }
    }
    if (successes) {
      messages.add(Text.translatable("blockPredicate.in_the_tag", EnhancedCommands.wrapBlockPos(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_TARGET), Text.literal("#" + blockTag.id().toString()).styled(EnhancedCommands.STYLE_FOR_EXPECTED)).formatted(Formatting.GREEN));
    }
    return new TestResult(successes, messages.build());
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.TAG;
  }

  public enum Type implements BlockPredicateType<TagBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(SuggestedParser parser0) throws CommandSyntaxException {
      SimpleBlockPredicateSuggestedParser parser = new SimpleBlockPredicateSuggestedParser(parser0);
      parser.parseBlockTagIdAndProperties();
      if (parser.tagId != null) {
        return new TagBlockPredicate(parser.tagId.getTag(), parser.propertyNameEntries);
      } else {
        return null;
      }
    }
  }
}
