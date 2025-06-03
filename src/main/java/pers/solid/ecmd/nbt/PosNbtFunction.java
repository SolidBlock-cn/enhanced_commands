package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.function.nbt.NbtFunctionArgument;
import pers.solid.ecmd.function.nbt.NbtFunctionType;
import pers.solid.ecmd.predicate.block.ExecutionContext;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.parse.FunctionLikeParser;
import pers.solid.ecmd.util.parse.ParseContext;

public record PosNbtFunction(Vec3d pos) implements NbtFunction {
  public static final MapCodec<PosNbtFunction> CODEC = Vec3d.CODEC.fieldOf("pos").xmap(PosNbtFunction::new, PosNbtFunction::pos);

  @Override
  public @NotNull String asString() {
    return "pos(" + StringUtil.wrapVector(pos) + ")";
  }

  @Override
  public @NotNull NbtFunctionType<PosNbtFunction> getType() {
    return Type.POS_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
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

  public static class Parser implements FunctionLikeParser<NbtFunctionArgument> {
    private PosArgument posArgument;

    @Override
    public NbtFunctionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return source -> new PosNbtFunction(posArgument.toAbsolutePos(source));
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      posArgument = parseContext.parseAndSuggestArgument(Vec3ArgumentType.vec3(true));
    }
  }
}
