package pers.solid.ecmd.block.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.property.predicate.PropertyNamePredicate;
import pers.solid.ecmd.util.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see BlockStateParser#readTag()
 */
public record TagBlockPredicate(HolderSet<Block> tag, @UnmodifiableView List<PropertyNamePredicate> properties) implements BlockPredicate {
  public static final Codec<TagBlockPredicate> STRING_BASED_CODEC = RegistryCodecs.homogeneousList(Registries.BLOCK, true).flatComapMap(TagBlockPredicate::new, tagBlockPredicate -> tagBlockPredicate.properties.isEmpty() ? DataResult.success(tagBlockPredicate.tag) : DataResult.error(() -> "cannot serialize predicate with properties to strings"));

  public static final MapCodec<TagBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(TagBlockPredicate::new, RegistryCodecs.homogeneousList(Registries.BLOCK, true).fieldOf("tag").forGetter(TagBlockPredicate::tag), PropertyNamePredicate.CODEC.listOf().optionalFieldOf("properties", Collections.emptyList()).forGetter(TagBlockPredicate::properties)));

  public TagBlockPredicate(HolderSet<Block> tag) {
    this(tag, Collections.emptyList());
  }

  @Override
  public String expressAsString() {
    final String tagString = "#" + tag.unwrapKey().map(blockTagKey -> DefaultNamespace.MINECRAFT.toSimplerString(blockTagKey.location())).orElse("[unregistered]");
    if (properties.isEmpty()) {
      return tagString;
    } else {
      return tagString + "[" + properties.stream().map(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockState blockState = blockInWorld.getState();
    final boolean inTag = blockState.is(tag);
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
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockState blockState = blockInWorld.getState();
    final boolean inTag = blockState.is(tag);
    boolean successes = true;
    ImmutableList.Builder<Component> messages = new ImmutableList.Builder<>();
    final String tagString = tag.unwrapKey().map(blockTagKey -> "#" + DefaultNamespace.MINECRAFT.toSimplerString(blockTagKey.location())).orElse("[unknown]");
    if (!inTag) {
      successes = false;
      messages.add(Component.translatable("enhanced_commands.block_predicate.tag.not_in_the_tag", TextUtil.wrapVector(blockInWorld.getPos()), blockState.getBlock().getName().withStyle(Styles.ACTUAL), Component.literal(tagString).withStyle(Styles.EXPECTED)).withStyle(Styles.FALSE));
    }
    for (PropertyNamePredicate propertyNamePredicate : properties) {
      final TestResult testResult = propertyNamePredicate.testAndDescribe(blockState, blockInWorld.getPos());
      messages.addAll(testResult.descriptions());
      if (!testResult.successes()) {
        successes = false;
      }
    }
    if (successes) {
      messages.add(Component.translatable("enhanced_commands.block_predicate.tag.in_the_tag", TextUtil.wrapVector(blockInWorld.getPos()), blockState.getBlock().getName().withStyle(Styles.TARGET), Component.literal(tagString).withStyle(Styles.EXPECTED)).withStyle(Styles.TRUE));
    }
    return new TestResult(successes, messages.build());
  }

  @Override
  public BlockPredicateType<TagBlockPredicate> getType() {
    return BlockPredicateTypes.TAG;
  }

  public enum TagParser implements Parser<TagBlockPredicate> {
    TAG_TYPE;

    @Override
    public @Nullable TagBlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      SimpleBlockPredicateParser<?> parser = new SimpleBlockPredicateParser<>(parseContext);
      parser.parseBlockTagIdAndProperties();
      if (parser.tagId != null) {
        return new TagBlockPredicate(parser.tagId, parser.propertyNamePredicates);
      } else {
        return null;
      }
    }
  }
}
