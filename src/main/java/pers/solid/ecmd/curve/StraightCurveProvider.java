package pers.solid.ecmd.curve;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.util.PositionProvider;

public record StraightCurveProvider(EnhancedCoordinates from, EnhancedCoordinates to) implements CurveProvider<StraightCurve> {
  public static final MapCodec<StraightCurveProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnhancedCoordinates.CODEC.fieldOf("from").forGetter(StraightCurveProvider::from),
      EnhancedCoordinates.CODEC.fieldOf("to").forGetter(StraightCurveProvider::to)
  ).apply(i, StraightCurveProvider::new));

  @Override
  public StraightCurve toAbsoluteRegion(PositionProvider positionProvider) throws CommandSyntaxException {
    return new StraightCurve(from.toAbsolutePos(positionProvider), to.toAbsolutePos(positionProvider));
  }

  @Override
  public @NotNull StraightCurve.Type getType() {
    return CurveTypes.STRAIGHT;
  }
}
