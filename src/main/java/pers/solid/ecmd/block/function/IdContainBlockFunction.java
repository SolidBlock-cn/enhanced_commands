package pers.solid.ecmd.block.function;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 从 id 包含指定正则表达式的方块中随机选择一个。
 */
public final class IdContainBlockFunction implements BlockFunction {
  public static final MapCodec<IdContainBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(IdContainBlockFunction::new, CodecUtil.PATTERN.fieldOf("pattern").forGetter(IdContainBlockFunction::pattern), CodecUtil.optionalLongFieldOf("seed").forGetter(IdContainBlockFunction::seed)));
  private final Pattern pattern;
  private final OptionalLong seed;
  private transient @Nullable Level world;
  private transient Block @Nullable [] blocks;

  public IdContainBlockFunction(Pattern pattern, OptionalLong seed) {
    this.pattern = pattern;
    this.seed = seed;
  }

  public OptionalLong seed() {
    return seed;
  }

  public Block[] getBlocks(Level world) {
    if (!world.equals(this.world) || blocks == null) {
      blocks = world.holderLookup(Registries.BLOCK).listElements().filter(reference -> pattern.matcher(reference.key().location().toString()).find()).map(Holder.Reference::value).toArray(Block[]::new);
      this.world = world;
    }
    return blocks;
  }

  @Override
  public String asString() {
    return "idcontain(" + StringTag.quoteAndEscape(pattern.toString()) + (seed.isPresent() ? ", seed = " + seed.getAsLong() : "") + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) {
    final Block[] blocks = getBlocks(level);
    final RandomSource random = context.getSplitterForOptionalSeed(this, seed).at(pos);
    if (blocks.length == 0) {
      return blockState;
    }
    return blocks[random.nextInt(blocks.length)].defaultBlockState();
  }

  @Override
  public Type getType() {
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
    int result = pattern.pattern().hashCode();
    result = 31 * result + seed.hashCode();
    return result;
  }

  public Pattern pattern() {
    return pattern;
  }

  public enum Type implements BlockFunctionType<IdContainBlockFunction> {
    ID_CONTAIN_TYPE;

    @Override
    public MapCodec<IdContainBlockFunction> getCodec() {
      return CODEC;
    }

  }

  public static class Parser implements FunctionContentParser.MixedParams<IdContainBlockFunction> {
    private static final Set<String> SUPPORTED_PARAM_NAMES = ImmutableSet.of("seed");
    private @Nullable Pattern pattern;
    private OptionalLong seed = OptionalLong.empty();

    @Override
    public IdContainBlockFunction getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(pattern, "pattern");
      return new IdContainBlockFunction(pattern, seed);
    }

    @Override
    public @Unmodifiable Collection<String> supportedParams() {
      return SUPPORTED_PARAM_NAMES;
    }

    @Override
    public boolean isDuplicateParamName(String paramName) {
      return paramName.equals("seed") && seed.isPresent();
    }

    @Override
    public void parseNamedParameter(String paramName, ParseContext<?> parseContext) throws CommandSyntaxException {
      if (paramName.equals("seed")) {
        seed = OptionalLong.of(parseContext.reader().readLong());
      }
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        pattern = ParsingUtil.readRegex(parseContext.reader());
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 1;
    }
  }
}
