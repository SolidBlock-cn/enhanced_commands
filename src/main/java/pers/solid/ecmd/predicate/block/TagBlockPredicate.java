package pers.solid.ecmd.predicate.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import pers.solid.ecmd.argument.SimpleBlockPredicateParser;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see BlockArgumentParser#parseTagId()
 */
public record TagBlockPredicate(@NotNull TagKey<Block> tag, @NotNull @UnmodifiableView List<PropertyNamePredicate> properties) implements BlockPredicate {
  public static final MapCodec<TagBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(TagBlockPredicate::new, TagKey.unprefixedCodec(RegistryKeys.BLOCK).fieldOf("tag").forGetter(TagBlockPredicate::tag), PropertyNamePredicate.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(TagBlockPredicate::properties)));

  public TagBlockPredicate(@NotNull TagKey<Block> tag) {
    this(tag, Collections.emptyList());
  }

  @Override
  public @NotNull String asString() {
    if (properties.isEmpty()) {
      return "#" + tag.id().toString();
    } else {
      return "#" + tag.id().toString() + "[" + properties.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final boolean inTag = blockState.isIn(tag);
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
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final boolean inTag = blockState.isIn(tag);
    boolean successes = true;
    ImmutableList.Builder<Text> messages = new ImmutableList.Builder<>();
    if (!inTag) {
      successes = false;
      messages.add(Text.translatable("enhanced_commands.block_predicate.tag.not_in_the_tag", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(Styles.ACTUAL), Text.literal("#" + tag.id().toString()).styled(Styles.EXPECTED)).styled(Styles.FALSE));
    }
    for (PropertyNamePredicate propertyNamePredicate : properties) {
      final TestResult testResult = propertyNamePredicate.testAndDescribe(blockState, cachedBlockPosition.getBlockPos());
      messages.addAll(testResult.descriptions());
      if (!testResult.successes()) {
        successes = false;
      }
    }
    if (successes) {
      messages.add(Text.translatable("enhanced_commands.block_predicate.tag.in_the_tag", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(Styles.TARGET), Text.literal("#" + tag.id().toString()).styled(Styles.EXPECTED)).styled(Styles.TRUE));
    }
    return new TestResult(successes, messages.build());
  }

  @Override
  public @NotNull Type getType() {
    return BlockPredicateTypes.TAG;
  }

  public enum Type implements BlockPredicateType<TagBlockPredicate>, Parser<BlockPredicateArgument> {
    TAG_TYPE;

    @Override
    public @NotNull MapCodec<TagBlockPredicate> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable TagBlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      SimpleBlockPredicateParser<?> parser = new SimpleBlockPredicateParser<>(parseContext);
      parser.parseBlockTagIdAndProperties();
      if (parser.tagId != null) {
        return new TagBlockPredicate(parser.tagId.getTag(), parser.propertyNamePredicates);
      } else {
        return null;
      }
    }
  }
}
