package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.function.nbt.NbtFunctionType;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;

public record PosNbtFunction(EnhancedPosArgument pos) implements NbtFunction {
  public static final MapCodec<PosNbtFunction> CODEC = EnhancedPosArgument.CODEC.fieldOf("pos").xmap(PosNbtFunction::new, PosNbtFunction::pos);

  @Override
  public @NotNull String asString() {
    return "pos(" + pos.asString() + ")";
  }

  @Override
  public @NotNull NbtFunctionType<PosNbtFunction> getType() {
    return Type.POS_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
    final Vec3d pos = this.pos.toAbsolutePos(context.positionProvider);
    final DataResult<NbtElement> result = Vec3d.CODEC.encodeStart(NbtOps.INSTANCE, pos);
    return result.getOrThrow(ModCommandExceptionTypes.CANNOT_PARSE::create);
  }

  public enum Type implements NbtFunctionType<PosNbtFunction> {
    POS_TYPE;

    @Override
    public MapCodec<PosNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser<NbtFunction> {
    private EnhancedPosArgument posArgument;

    @Override
    public NbtFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new PosNbtFunction(posArgument);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      posArgument = parseContext.parseAndSuggestArgument(EnhancedPosArgumentType.posPreferringCenteredInt());
    }
  }
}
