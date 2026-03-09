package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtFunctionParser;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

public record NbtBlockFunction(@NotNull NbtFunction nbtFunction) implements BlockFunction {
  public static final MapCodec<NbtBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(NbtBlockFunction::new, NbtFunction.CODEC.fieldOf("nbt").forGetter(NbtBlockFunction::nbtFunction)));
  public static final DynamicCommandExceptionType NOT_COMPOUND = new DynamicCommandExceptionType(s -> Component.translatable("enhanced_commands.block_function.nbt_not_compound", s));

  @Override
  public @NotNull String asString() {
    return nbtFunction.asString();
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    try {
      final Tag applied = nbtFunction.apply(blockEntityData.getValue(), context);
      if (applied instanceof CompoundTag nbtCompound) {
        blockEntityData.setValue(nbtCompound);
      } else {
        throw NOT_COMPOUND.create(applied.getType().getPrettyName());
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
