package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.argument.MirrorProvider;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Objects;

public record MirrorBlockFunction(MirrorProvider mirror) implements BlockFunction {
  public static final MapCodec<MirrorBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(MirrorBlockFunction::new, MirrorProvider.CODEC.fieldOf("mirror").forGetter(MirrorBlockFunction::mirror)));

  @Override
  public String expressAsString() {
    return "mirror(" + mirror.getSerializedName() + ")";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@org.jetbrains.annotations.Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    return blockState.mirror(mirror.apply((CommandSourceStack) context.positionProvider));
  }

  @Override
  public BlockFunctionType<MirrorBlockFunction> getType() {
    return BlockFunctionTypes.MIRROR;
  }

  public static class Parser implements FunctionContentParser.SequentialParams<MirrorBlockFunction> {
    private @Nullable MirrorProvider mirror;

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 1;
    }

    @Override
    public MirrorBlockFunction getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(mirror, "mirror");
      return new MirrorBlockFunction(mirror);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      mirror = parseContext.parseAndSuggestEnums(MirrorProvider.values(), mirrorProvider -> null, MirrorProvider.CODEC);
    }
  }
}
