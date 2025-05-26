package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.NamedParamListParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collection;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 从 id 包含指定正则表达式的方块中随机选择一个。
 */
public final class IdContainBlockFunction implements BlockFunction {
  public static final MapCodec<IdContainBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(IdContainBlockFunction::new, CodecUtil.PATTERN.fieldOf("pattern").forGetter(IdContainBlockFunction::pattern), CodecUtil.optionalLongFieldOf("seed").forGetter(IdContainBlockFunction::seed)));
  private final @NotNull Pattern pattern;
  private final OptionalLong seed;
  private transient World world;
  private transient Block[] blocks;

  public IdContainBlockFunction(@NotNull Pattern pattern, OptionalLong seed) {
    this.pattern = pattern;
    this.seed = seed;
  }

  public OptionalLong seed() {
    return seed;
  }

  public @NotNull Block[] getBlocks(@NotNull World world) {
    if (!world.equals(this.world)) {
      blocks = world.createCommandRegistryWrapper(RegistryKeys.BLOCK).streamEntries().filter(reference -> pattern.matcher(reference.registryKey().getValue().toString()).find()).map(RegistryEntry.Reference::value).toArray(Block[]::new);
      this.world = world;
    }
    return blocks;
  }

  @Override
  public @NotNull String asString() {
    return "idcontain(" + NbtString.escape(pattern.toString()) + (seed.isPresent() ? "; seed = " + seed.getAsLong() : "") + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final Block[] blocks = getBlocks(world);
    final Random random = context.getSplitterForOptionalSeed(this, seed).split(pos);
    if (blocks.length == 0) {
      return blockState;
    }
    return blocks[random.nextInt(blocks.length)].getDefaultState();
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.ID_CONTAIN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof IdContainBlockFunction that))
      return false;

    return pattern.pattern().equals(that.pattern.pattern());
  }

  @Override
  public int hashCode() {
    int result = pattern.hashCode();
    result = 31 * result + seed.hashCode();
    return result;
  }

  public @NotNull Pattern pattern() {
    return pattern;
  }

  public enum Type implements BlockFunctionType<IdContainBlockFunction> {
    ID_CONTAIN_TYPE;

    @Override
    public @NotNull MapCodec<IdContainBlockFunction> getCodec() {
      return CODEC;
    }

  }

  public static class Parser implements FunctionLikeParser<BlockFunctionArgument>, NamedParamListParser {
    private static final Set<String> SUPPORTED_PARAM_NAMES = ImmutableSet.of("seed");
    private Pattern pattern;
    private OptionalLong seed = OptionalLong.empty();

    @Override
    public IdContainBlockFunction getParseResult(ParseContext<?> parseContext) {
      return new IdContainBlockFunction(pattern, seed);
    }

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAM_NAMES;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return seed.isPresent();
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      seed = OptionalLong.of(parseContext.reader().readLong());
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      pattern = ParsingUtil.readRegex(reader);
      if (reader.canRead() && reader.peek() == ';') {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        parseNamedParameters(parseContext);
      }
    }
  }
}
