package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.NamedParamListParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;

import java.util.Collection;
import java.util.OptionalLong;
import java.util.Set;

/**
 * 此方块函数可以产生任意的方块的任意方块状态，无论其原先的方块是什么。
 */
public final class RandomBlockFunction implements BlockFunction {
  public static final MapCodec<RandomBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(CodecUtil.optionalLongFieldOf("seed").forGetter(RandomBlockFunction::seed)).apply(i, RandomBlockFunction::new));
  private transient FeatureSet featureSet;
  private transient Block[] blocks;
  private final OptionalLong seed;
  public static final RandomBlockFunction RANDOM_SEED = new RandomBlockFunction(OptionalLong.empty());

  public OptionalLong seed() {
    return seed;
  }

  public RandomBlockFunction(OptionalLong seed) {
    this.seed = seed;
  }

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
    if (seed.isPresent()) {
      return "random(seed = " + seed.getAsLong() + ")";
    }
    return "*";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final Block[] blocks = getBlocks(world.getRegistryManager(), world.getEnabledFeatures());
    if (blocks.length == 0) {
      return blockState;
    }
    final Random random = context.getSplitterForOptionalSeed(this, seed).split(pos);
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

  public enum Type implements BlockFunctionType<RandomBlockFunction>, Parser<RandomBlockFunction> {
    RANDOM_TYPE;

    @Override
    public @NotNull MapCodec<RandomBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable RandomBlockFunction parse(ParseContext<?> parseContext) {
      final StringReader reader = parseContext.reader();
      if (reader.getRemaining().isEmpty()) {
        parseContext.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder.suggest("*", Text.translatable("enhanced_commands.block_function.random")).buildFuture());
      }
      if (reader.canRead() && reader.peek() == '*') {
        reader.skip();
        parseContext.clearSuggestion();
        return RANDOM_SEED;
      }
      return null;
    }
  }

  public static class RandFuncParser implements FunctionLikeParser<RandomBlockFunction>, NamedParamListParser {
    protected OptionalLong seed = OptionalLong.empty();
    private static final Set<String> SUPPORTED_PARAMS = Set.of("seed");

    @Override
    public RandomBlockFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new RandomBlockFunction(seed);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.clearSuggestion();
      parseNamedParameters(parseContext);
    }

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAMS;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return seed.isPresent();
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      seed = OptionalLong.of(parseContext.reader().readLong());
    }
  }
}
