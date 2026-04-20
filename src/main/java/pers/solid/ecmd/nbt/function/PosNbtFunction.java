package pers.solid.ecmd.nbt.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Objects;

public record PosNbtFunction(EnhancedCoordinates pos) implements NbtFunction {
  public static final MapCodec<PosNbtFunction> CODEC = EnhancedCoordinates.CODEC.fieldOf("pos").xmap(PosNbtFunction::new, PosNbtFunction::pos);

  @Override
  public String asString() {
    return "pos(" + pos.asString() + ")";
  }

  @Override
  public NbtFunctionType<PosNbtFunction> getType() {
    return NbtFunctionTypes.POS;
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    final Vec3 pos = this.pos.toAbsolutePos(context.positionProvider);
    final DataResult<Tag> result = Vec3.CODEC.encodeStart(NbtOps.INSTANCE, pos);
    return result.getOrThrow(EnhancedCommandsCommandExceptionTypes.CANNOT_PARSE::create);
  }

  public static class Parser implements FunctionContentParser<NbtFunction> {
    private @Nullable EnhancedCoordinates enhancedCoordinates;

    @Override
    public NbtFunction getParseResult(ParseContext<?> parseContext) {
      return new PosNbtFunction(Objects.requireNonNull(enhancedCoordinates, "enhancedCoordinates"));
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      enhancedCoordinates = parseContext.parseAndSuggestArgument(EnhancedPosArgument.posPreferringCenteredInt());
    }
  }
}
