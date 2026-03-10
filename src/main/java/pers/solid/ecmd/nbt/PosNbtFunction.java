package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.function.nbt.NbtFunctionType;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ExecutionContext;

public record PosNbtFunction(EnhancedCoordinates pos) implements NbtFunction {
  public static final MapCodec<PosNbtFunction> CODEC = EnhancedCoordinates.CODEC.fieldOf("pos").xmap(PosNbtFunction::new, PosNbtFunction::pos);

  @Override
  public @NotNull String asString() {
    return "pos(" + pos.asString() + ")";
  }

  @Override
  public @NotNull NbtFunctionType<PosNbtFunction> getType() {
    return Type.POS_TYPE;
  }

  @Override
  public @NotNull Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    final Vec3 pos = this.pos.toAbsolutePos(context.positionProvider);
    final DataResult<Tag> result = Vec3.CODEC.encodeStart(NbtOps.INSTANCE, pos);
    return result.getOrThrow(EnhancedCommandsCommandExceptionTypes.CANNOT_PARSE::create);
  }

  public enum Type implements NbtFunctionType<PosNbtFunction> {
    POS_TYPE;

    @Override
    public MapCodec<PosNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionContentParser<NbtFunction> {
    private EnhancedCoordinates posArgument;

    @Override
    public NbtFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new PosNbtFunction(posArgument);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      posArgument = parseContext.parseAndSuggestArgument(EnhancedPosArgument.posPreferringCenteredInt());
    }
  }
}
