package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

public enum UseOriginalBlockFunction implements BlockFunction {
  USE_ORIGINAL;

  public static final MapCodec<UseOriginalBlockFunction> CODEC = MapCodec.unit(USE_ORIGINAL);

  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    return origState;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.USE_ORIGINAL;
  }

  public enum Type implements BlockFunctionType<UseOriginalBlockFunction>, Parser<BlockFunctionArgument> {
    USE_ORIGINAL_TYPE;

    @Override
    public @NotNull MapCodec<UseOriginalBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable UseOriginalBlockFunction parse(ParseContext<?> parseContext) {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("~", Text.translatable("enhanced_commands.block_function.use_original"), suggestionsBuilder).buildFuture());
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == '~') {
        reader.skip();
        parseContext.clearSuggestion();
        return USE_ORIGINAL;
      }
      return null;
    }
  }
}
