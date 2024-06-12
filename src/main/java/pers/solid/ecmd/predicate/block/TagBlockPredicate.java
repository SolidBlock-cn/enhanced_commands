package pers.solid.ecmd.predicate.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see BlockArgumentParser#parseTagId()
 */
public record TagBlockPredicate(@NotNull TagKey<Block> tags, @NotNull @UnmodifiableView List<PropertyNamePredicate> properties) implements BlockPredicate {
  public static final Codec<TagBlockPredicate> CODEC = RecordCodecBuilder.create(i -> i.apply2(TagBlockPredicate::new, TagKey.unprefixedCodec(RegistryKeys.BLOCK).fieldOf("tag").forGetter(TagBlockPredicate::tags), PropertyNamePredicate.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(TagBlockPredicate::properties)));

  @Override
  public @NotNull String asString() {
    if (properties.isEmpty()) {
      return "#" + tags.id().toString();
    } else {
      return "#" + tags.id().toString() + "[" + properties.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final boolean inTag = blockState.isIn(tags);
    if (!inTag) {
      return false;
    }
    for (PropertyNamePredicate propertyNamePredicate : properties) {
      if (!propertyNamePredicate.test(blockState))
        return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final boolean inTag = blockState.isIn(tags);
    boolean successes = true;
    ImmutableList.Builder<Text> messages = new ImmutableList.Builder<>();
    if (!inTag) {
      successes = false;
      messages.add(Text.translatable("enhanced_commands.block_predicate.tag.not_in_the_tag", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(Styles.ACTUAL), Text.literal("#" + tags.id().toString()).styled(Styles.EXPECTED)).styled(Styles.FALSE));
    }
    for (PropertyNamePredicate propertyNamePredicate : properties) {
      final TestResult testResult = propertyNamePredicate.testAndDescribe(blockState, cachedBlockPosition.getBlockPos());
      messages.addAll(testResult.descriptions());
      if (!testResult.successes()) {
        successes = false;
      }
    }
    if (successes) {
      messages.add(Text.translatable("enhanced_commands.block_predicate.tag.in_the_tag", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(Styles.TARGET), Text.literal("#" + tags.id().toString()).styled(Styles.EXPECTED)).styled(Styles.TRUE));
    }
    return new TestResult(successes, messages.build());
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.TAG;
  }

  public enum Type implements BlockPredicateType<TagBlockPredicate>, Parser<BlockPredicateArgument> {
    TAG_TYPE;

    @Override
    public @NotNull Codec<TagBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable TagBlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser0, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      SimpleBlockPredicateSuggestedParser parser = new SimpleBlockPredicateSuggestedParser(commandRegistryAccess, parser0);
      parser.parseBlockTagIdAndProperties();
      if (parser.tagId != null) {
        return new TagBlockPredicate(parser.tagId.getTag(), parser.propertyNamePredicates);
      } else {
        return null;
      }
    }
  }
}
