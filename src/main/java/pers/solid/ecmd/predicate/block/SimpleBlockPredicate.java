package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.property.PropertyPredicate;
import pers.solid.ecmd.util.NbtConvertible;
import pers.solid.ecmd.util.TextUtil;

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
    ImmutableList.Builder<Text> messages = new ImmutableList.Builder<>();
    if (!blockState.isOf(block)) {
      messages.add(Text.translatable("enhanced_commands.argument.block_predicate.not_the_block", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_ACTUAL), block.getName().styled(TextUtil.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
      matches = false;
    }
    for (PropertyPredicate<?> propertyPredicate : propertyEntries) {
      if (!propertyPredicate.test(blockState)) {
        final Property<?> property = propertyPredicate.property();
        if (blockState.contains(property)) {
          messages.add(Text.translatable("enhanced_commands.argument.block_predicate.property_not_this_value", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), expressPropertyValue(blockState, property).styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(propertyPredicate).styled(TextUtil.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
        } else {
          messages.add(Text.translatable("enhanced_commands.argument.block_predicate.expected_property_does_not_exist", TextUtil.wrapVector(cachedBlockPosition.getBlockPos()), Text.literal(property.getName()).styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(propertyPredicate).styled(TextUtil.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
        }
        matches = false;
      }
    }
    if (matches) {
      return TestResult.success(cachedBlockPosition.getBlockPos());
    } else {
      return new TestResult(false, messages.build());
    }
  }

  private <T extends Comparable<T>> MutableText expressPropertyValue(BlockState blockState, Property<T> property) {
    return Text.literal(property.getName() + "=" + (blockState.contains(property) ? property.name(blockState.get(property)) : "!"));
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

  public enum Type implements BlockPredicateType<SimpleBlockPredicate> {
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
