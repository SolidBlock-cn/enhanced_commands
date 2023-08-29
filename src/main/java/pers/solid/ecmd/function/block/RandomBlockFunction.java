package pers.solid.ecmd.function.block;

import com.google.common.base.Suppliers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.StateUtil;

import java.util.function.Supplier;

/**
 * 此方块函数可以产生任意的方块的任意方块状态，无论其原先的方块是什么。
 */
public final class RandomBlockFunction implements BlockFunction {
  private transient final Supplier<Block[]> blocks;

  /**
   *
   */
  public RandomBlockFunction(RegistryWrapper<Block> registryWrapper) {
    blocks = Suppliers.memoize(() -> registryWrapper.streamEntries().map(RegistryEntry.Reference::value).toArray(Block[]::new));
  }

  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    final Block[] blocks = this.blocks.get();
    if (blocks.length == 0) {
      return null;
    }
    final Random random = world.getRandom();
    final Block block = blocks[random.nextInt(blocks.length)];
    return StateUtil.getBlockWithRandomProperties(block, random);
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {}

  @Override
  public @NotNull BlockFunctionType<RandomBlockFunction> getType() {
    return BlockFunctionTypes.RANDOM;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    return o instanceof RandomBlockFunction;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return "RandomBlockFunction{}";
  }

  public enum Type implements BlockFunctionType<RandomBlockFunction> {
    RANDOM_TYPE;

    @Override
    public RandomBlockFunction fromNbt(NbtCompound nbtCompound) {
      return new RandomBlockFunction(Registries.BLOCK.getReadOnlyWrapper());
    }

    @Override
    public @Nullable RandomBlockFunction parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) {
      if (parser.reader.getRemaining().isEmpty()) {
        parser.suggestions.add((context, suggestionsBuilder) -> suggestionsBuilder.suggest("*", Text.translatable("enhancedCommands.argument.block_function.random")));
      }
      if (parser.reader.canRead() && parser.reader.peek() == '*') {
        parser.reader.skip();
        parser.suggestions.clear();
        return new RandomBlockFunction(commandRegistryAccess.createWrapper(RegistryKeys.BLOCK));
      }
      return null;
    }
  }
}
