package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtFunctionParser;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record NbtBlockFunction(@NotNull NbtFunction nbtFunction) implements BlockFunction {
  public static final MapCodec<NbtBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(NbtBlockFunction::new, NbtFunction.CODEC.fieldOf("nbt").forGetter(NbtBlockFunction::nbtFunction)));
  public static final DynamicCommandExceptionType NOT_COMPOUND = new DynamicCommandExceptionType(s -> Text.translatable("enhanced_commands.block_function.nbt_not_compound", s));

  @Override
  public @NotNull String asString() {
    return nbtFunction.asString();
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    try {
      final NbtElement applied = nbtFunction.apply(blockEntityData.getValue(), context);
      if (applied instanceof NbtCompound nbtCompound) {
        blockEntityData.setValue(nbtCompound);
      } else {
        throw NOT_COMPOUND.create(applied.getNbtType().getCommandFeedbackName());
      }
    } catch (CommandSyntaxException e) {
      throw new CommandRuntimeException(e);
    }
    return blockState;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.NBT;
  }

  public enum Type implements BlockFunctionType<NbtBlockFunction>, Parser<NbtBlockFunction> {
    NBT_TYPE;

    @Override
    public @NotNull MapCodec<NbtBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public NbtBlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("{", NbtPredicateParser.START_OF_COMPOUND, suggestionsBuilder).buildFuture());
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == '{') {
        final NbtFunction nbtFunctionArgument = new NbtFunctionParser<>(parseContext).parsePreferringCompound(false, false);
        return new NbtBlockFunction(nbtFunctionArgument);
      } else {
        return null;
      }
    }
  }
}
