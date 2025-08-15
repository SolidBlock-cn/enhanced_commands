package pers.solid.ecmd.curve;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.util.PositionProvider;

public record StraightCurveArgument(EnhancedPosArgument from, EnhancedPosArgument to) implements CurveArgument<StraightCurve> {
  public static final MapCodec<StraightCurveArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnhancedPosArgument.CODEC.fieldOf("from").forGetter(StraightCurveArgument::from),
      EnhancedPosArgument.CODEC.fieldOf("to").forGetter(StraightCurveArgument::to)
  ).apply(i, StraightCurveArgument::new));

  @Override
  public StraightCurve toAbsoluteRegion(PositionProvider positionProvider) throws CommandSyntaxException {
    return new StraightCurve(from.toAbsolutePos(positionProvider), to.toAbsolutePos(positionProvider));
  }

  @Override
  public @NotNull StraightCurve.Type getType() {
    return CurveTypes.STRAIGHT;
  }
}
