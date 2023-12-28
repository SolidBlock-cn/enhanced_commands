package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.property.PropertyPredicate;
import pers.solid.ecmd.util.NbtConvertible;
import pers.solid.ecmd.util.Parser;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record SimpleBlockPredicate(Block block, Collection<PropertyPredicate<?>> propertyEntries) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return Registries.BLOCK.getId(block).toString();
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    if (!cachedBlockPosition.getBlockState().isOf(block))
      return false;
    for (PropertyPredicate<?> propertyPredicate : propertyEntries) {
      if (!propertyPredicate.test(cachedBlockPosition.getBlockState()))
        return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    boolean matches = true;
    final BlockState blockState = cachedBlockPosition.getBlockState();
    final List<Text> messages = new ArrayList<>();
    final BlockPos blockPos = cachedBlockPosition.getBlockPos();
    final MutableText posText = TextUtil.wrapVector(blockPos);
    final MutableText actualText = blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_ACTUAL);
    if (!blockState.isOf(block)) {
      final MutableText expectedText = block.getName().styled(TextUtil.STYLE_FOR_EXPECTED);
      messages.add(Text.translatable("enhanced_commands.argument.block_predicate.simple.not_the_block", posText, actualText, expectedText).formatted(Formatting.RED));
      matches = false;
    } else {
      messages.add(Text.translatable("enhanced_commands.argument.block_predicate.simple.is_the_block", posText, actualText).formatted(Formatting.GREEN));
    }
    for (PropertyPredicate<?> propertyPredicate : propertyEntries) {
      final TestResult propertyResult = propertyPredicate.testAndDescribe(blockState, blockPos);
      messages.addAll(propertyResult.descriptions());
      if (!propertyResult.successes()) {
        matches = false;
      }
    }
    return new TestResult(matches, messages);
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.SIMPLE;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("block", Registries.BLOCK.getId(block).toString());
    if (!propertyEntries.isEmpty()) {
      final NbtList nbtList = new NbtList();
      nbtCompound.put("properties", nbtList);
      nbtList.addAll(Collections2.transform(propertyEntries, NbtConvertible::createNbt));
    }
  }

  public enum Type implements BlockPredicateType<SimpleBlockPredicate>, Parser<BlockPredicateArgument> {
    SIMPLE_TYPE;

    @Override
    public @NotNull SimpleBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final Block block = Registries.BLOCK.getOrEmpty(new Identifier(nbtCompound.getString("block"))).orElseThrow();
      final List<PropertyPredicate<?>> predicates = nbtCompound.getList("properties", NbtElement.COMPOUND_TYPE)
          .stream()
          .<PropertyPredicate<?>>map(nbtElement -> PropertyPredicate.fromNbt((NbtCompound) nbtElement, block))
          .toList();
      return new SimpleBlockPredicate(block, predicates);
    }

    @Override
    public @NotNull BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser0, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      SimpleBlockPredicateSuggestedParser parser = new SimpleBlockPredicateSuggestedParser(commandRegistryAccess, parser0);
      parser.parseBlockId();
      parser.parseProperties();
      return new SimpleBlockPredicate(parser.block, parser.propertyPredicates);
    }
  }
}
