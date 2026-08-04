package pers.solid.ecmd.block.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.Collections;

public enum UseOriginalBlockFunction implements BlockFunction {
  USE_ORIGINAL;

  public static final MapCodec<UseOriginalBlockFunction> CODEC = MapCodec.unit(USE_ORIGINAL);

  @Override
  public String expressAsString() {
    return "~";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    return originalState;
  }

  @Override
  public BlockFunctionType<UseOriginalBlockFunction> getType() {
    return BlockFunctionTypes.USE_ORIGINAL;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return Collections.emptyList();
  }

  public enum WaveParser implements Parser<UseOriginalBlockFunction> {
    INSTANCE;

    @Override
    public @Nullable UseOriginalBlockFunction parse(ParseContext<?> parseContext) {
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
