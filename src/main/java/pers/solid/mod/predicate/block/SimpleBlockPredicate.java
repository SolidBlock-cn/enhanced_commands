package pers.solid.mod.predicate.block;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import pers.solid.mod.EnhancedCommands;
import pers.solid.mod.argument.BlockPredicateArgumentParser;
import pers.solid.mod.command.TestResult;
import pers.solid.mod.predicate.property.PropertyEntry;

import java.util.Collection;

public record SimpleBlockPredicate(Block block, Collection<PropertyEntry<?>> propertyEntries) implements BlockPredicate {
  @Override
  public String asString() {
    return Registries.BLOCK.getId(block).toString();
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    if (!cachedBlockPosition.getBlockState().isOf(block)) return false;
    for (PropertyEntry<?> propertyEntry : propertyEntries) {
      if (!propertyEntry.test(cachedBlockPosition.getBlockState())) return false;
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    boolean matches = true;
    final BlockState blockState = cachedBlockPosition.getBlockState();
    ImmutableList.Builder<Text> messages = new ImmutableList.Builder<>();
    if (!blockState.isOf(block)) {
      messages.add(Text.translatable("blockPredicate.not_the_block", EnhancedCommands.wrapBlockPos(cachedBlockPosition.getBlockPos()), blockState.getBlock().getName().styled(EnhancedCommands.STYLE_FOR_ACTUAL), block.getName().styled(EnhancedCommands.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
      matches = false;
    }
    for (PropertyEntry<?> propertyEntry : propertyEntries) {
      if (!propertyEntry.test(blockState)) {
        final Property<?> property = propertyEntry.property();
        if (blockState.contains(property)) {
          messages.add(Text.translatable("blockPredicate.property_not_this_value", EnhancedCommands.wrapBlockPos(cachedBlockPosition.getBlockPos()), expressPropertyValue(blockState, property).styled(EnhancedCommands.STYLE_FOR_ACTUAL), Text.literal(propertyEntry.asString()).styled(EnhancedCommands.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
        } else {
          messages.add(Text.translatable("blockPredicate.expected_property_does_not_exist", EnhancedCommands.wrapBlockPos(cachedBlockPosition.getBlockPos()), Text.literal(property.getName()).styled(EnhancedCommands.STYLE_FOR_ACTUAL), Text.literal(propertyEntry.asString()).styled(EnhancedCommands.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
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
    return Text.literal(property.getName() + "=" +( blockState.contains(property) ? property.name(blockState.get(property)) : "!"));
  }

  @Override
  public BlockPredicateType<?> getType() {
    return BlockPredicateTypes.SIMPLE;
  }

  @Override
  public void writeNbt(NbtCompound nbtCompound) {
    nbtCompound.putString("block", Registries.BLOCK.getId(block).toString());
  }

  public enum Type implements BlockPredicateType<SimpleBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(BlockPredicateArgumentParser parser) throws CommandSyntaxException {
      parser.suggestions.add(suggestionsBuilder -> CommandSource.forEachMatching(parser.registryWrapper.streamKeys().map(RegistryKey::getValue)::iterator, suggestionsBuilder.getRemaining().toLowerCase(), id -> id, id -> suggestionsBuilder.suggest(id.toString())));
      if (parser.reader.canRead() && Identifier.isCharValid(parser.reader.peek())) {
        parser.parseBlockId();
        parser.parseProperties();
        return new SimpleBlockPredicate(parser.block, parser.propertyEntries);
      }
      return null;
    }
  }
}
