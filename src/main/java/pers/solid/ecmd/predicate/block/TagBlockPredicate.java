package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.NbtConvertible;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see BlockArgumentParser#parseTagId()
 */
public record TagBlockPredicate(@NotNull TagKey<Block> blockTag, @NotNull @UnmodifiableView Collection<PropertyNamePredicate> propertyNamePredicates) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    if (propertyNamePredicates.isEmpty()) {
      return "#" + blockTag.id().toString();
    } else {
      return "#" + blockTag.id().toString() + "[" + propertyNamePredicates.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + "]";
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final boolean inTag = blockState.isIn(blockTag);
    if (!inTag) {
      return false;
    }
    for (PropertyNamePredicate propertyNamePredicate : propertyNamePredicates) {
      if (!propertyNamePredicate.test(blockState))
        return false;
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
      messages.add(Text.translatable("enhancedCommands.argument.block_predicate.not_in_the_tag", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_ACTUAL), Text.literal("#" + blockTag.id().toString()).styled(TextUtil.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
    }
    for (PropertyNamePredicate propertyNamePredicate : propertyNamePredicates) {
      final TestResult testResult = propertyNamePredicate.testAndDescribe(blockState, cachedBlockPosition.getBlockPos());
      if (!testResult.successes()) {
        messages.addAll(testResult.descriptions());
        successes = false;
      }
    }
    if (successes) {
      messages.add(Text.translatable("enhancedCommands.argument.block_predicate.in_the_tag", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), Text.literal("#" + blockTag.id().toString()).styled(TextUtil.STYLE_FOR_EXPECTED)).formatted(Formatting.GREEN));
    }
    return new TestResult(successes, messages.build());
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.TAG;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("tag", blockTag.id().toString());
    if (!propertyNamePredicates.isEmpty()) {
      final NbtList nbtList = new NbtList();
      nbtCompound.put("properties", nbtList);
      nbtList.addAll(Collections2.transform(propertyNamePredicates, NbtConvertible::createNbt));
    }
  }

  public enum Type implements BlockPredicateType<TagBlockPredicate> {
    TAG_TYPE;

    @Override
    public @NotNull TagBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, new Identifier(nbtCompound.getString("tag")));
      final List<PropertyNamePredicate> predicates = nbtCompound.getList("properties", NbtElement.COMPOUND_TYPE)
          .stream()
          .map(nbtElement -> PropertyNamePredicate.fromNbt((NbtCompound) nbtElement))
          .toList();
      return new TagBlockPredicate(tag, predicates);
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
