package pers.solid.ecmd.block.function;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

public enum UseOriginalBlockFunction implements BlockFunction {
  USE_ORIGINAL;

  public static final MapCodec<UseOriginalBlockFunction> CODEC = MapCodec.unit(USE_ORIGINAL);

  @Override
  public @NotNull String asString() {
    return "~";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    return originalState;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.USE_ORIGINAL;
  }

  public enum Type implements BlockFunctionType<UseOriginalBlockFunction>, Parser<UseOriginalBlockFunction> {
    USE_ORIGINAL_TYPE;

    @Override
    public @NotNull MapCodec<UseOriginalBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public UseOriginalBlockFunction parse(ParseContext<?> parseContext) {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("~", Component.translatable("enhanced_commands.block_function.use_original"), suggestionsBuilder).buildFuture());
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
