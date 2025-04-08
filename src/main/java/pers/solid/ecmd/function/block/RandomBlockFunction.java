package pers.solid.ecmd.function.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.parse.Parser;

/**
 * 此方块函数可以产生任意的方块的任意方块状态，无论其原先的方块是什么。
 */
public final class RandomBlockFunction implements BlockFunction {
  public static final MapCodec<RandomBlockFunction> CODEC = MapCodec.unit(RandomBlockFunction::new);
  private transient FeatureSet featureSet;
  private transient Block[] blocks;

  private @NotNull Block[] getBlocks(DynamicRegistryManager rm, FeatureSet fs) {
    if (blocks == null || featureSet != fs) {
      return (blocks = calculateBlocks(rm, fs));
    }
    return blocks;
  }

  private @NotNull Block[] calculateBlocks(DynamicRegistryManager rm, FeatureSet fs) {
    this.featureSet = fs;
    return rm.get(RegistryKeys.BLOCK).stream().filter(block -> block.isEnabled(fs)).toArray(Block[]::new);
  }

  @Override
  public @NotNull String asString() {
    return "*";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final Block[] blocks = getBlocks(world.getRegistryManager(), world.getEnabledFeatures());
    if (blocks.length == 0) {
      return blockState;
    }
    final Random random = context.getSplitter(this).split(pos);
    final Block block = blocks[random.nextInt(blocks.length)];
    return StateUtil.getBlockWithRandomProperties(block, random);
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.RANDOM;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
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

  public enum Type implements BlockFunctionType<RandomBlockFunction>, Parser<BlockFunctionArgument> {
    RANDOM_TYPE;

    @Override
    public @NotNull MapCodec<RandomBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable RandomBlockFunction parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowsSparse) {
      if (parser.reader.getRemaining().isEmpty()) {
        parser.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder.suggest("*", Text.translatable("enhanced_commands.block_function.random")).buildFuture());
      }
      if (parser.reader.canRead() && parser.reader.peek() == '*') {
        parser.reader.skip();
        parser.clearSuggestion();
        return new RandomBlockFunction();
      }
      return null;
    }
  }
}
