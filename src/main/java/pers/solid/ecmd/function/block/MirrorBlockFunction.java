package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.MirrorArgument;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

public record MirrorBlockFunction(@NotNull MirrorArgument mirror) implements BlockFunction {
  public static final MapCodec<MirrorBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(MirrorBlockFunction::new, MirrorArgument.CODEC.fieldOf("mirror").forGetter(MirrorBlockFunction::mirror)));

  @Override
  public @NotNull String asString() {
    return "mirror(" + mirror.asString() + ")";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    return blockState.mirror(mirror.apply((ServerCommandSource) context.positionProvider));
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.MIRROR;
  }

  public enum Type implements BlockFunctionType<MirrorBlockFunction> {
    MIRROR_TYPE;

    @Override
    public @NotNull MapCodec<MirrorBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionParamsParser<MirrorBlockFunction> {
    private MirrorArgument mirror;

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }

    @Override
    public MirrorBlockFunction getParseResult(ParseContext<?> parseContext) {
      return new MirrorBlockFunction(mirror);
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      mirror = parseContext.parseAndSuggestEnums(MirrorArgument.values(), mirrorArgument -> null, MirrorArgument.CODEC);
    }
  }
}
