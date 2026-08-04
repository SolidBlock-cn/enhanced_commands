package pers.solid.ecmd.block.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 对方块的 id 进行替换。如果方块的 id 替换后不存在，则不进行修改。例如：
 * <pre>
 *   idreplace('wool', terracotta)
 *   idreplace('_planks$', 'wood')
 * </pre>
 */
public record IdReplaceBlockFunction(Pattern pattern, String replacement) implements BlockFunction {
  public static final MapCodec<IdReplaceBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(IdReplaceBlockFunction::new, ExtraCodecs.PATTERN.fieldOf("pattern").forGetter(IdReplaceBlockFunction::pattern), Codec.STRING.fieldOf("replacement").forGetter(IdReplaceBlockFunction::replacement)));

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof IdReplaceBlockFunction that))
      return false;
    // 忽略 registryWrapper
    return pattern.pattern().equals(that.pattern.pattern()) && replacement.equals(that.replacement);
  }

  @Override
  public int hashCode() {
    // 忽略 registryWrapper
    return 31 * pattern.pattern().hashCode() + replacement.hashCode();
  }

  @Override
  public String expressAsString() {
    return "idreplace(" + StringTag.quoteAndEscape(pattern.pattern()) + ", " + StringTag.quoteAndEscape(replacement) + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    final Block block = blockState.getBlock();
    final String old = BuiltInRegistries.BLOCK.getKey(block).toString();
    final String replaced = pattern.matcher(old).replaceAll(replacement);
    final ResourceLocation identifier = ResourceLocation.tryParse(replaced);
    if (identifier == null) return blockState;
    return level.registryAccess().lookupOrThrow(Registries.BLOCK).getOptional(identifier).filter(block1 -> block1.isEnabled(level.enabledFeatures())).map(Block::defaultBlockState).orElse(blockState);
  }

  @Override
  public BlockFunctionType<IdReplaceBlockFunction> getType() {
    return BlockFunctionTypes.ID_REPLACE;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return Collections.emptyList();
  }

  public static class Parser implements FunctionContentParser.SequentialParams<IdReplaceBlockFunction> {
    private @Nullable Pattern pattern;
    private @Nullable String replacement;

    public Parser() {
    }

    @Override
    public IdReplaceBlockFunction getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(pattern, "pattern");
      Objects.requireNonNull(replacement, "replacement");
      return new IdReplaceBlockFunction(pattern, replacement);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        pattern = ParsingUtil.readRegex(reader);
      } else if (paramIndex == 1) {
        replacement = reader.readString();
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return 2;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }
  }
}
