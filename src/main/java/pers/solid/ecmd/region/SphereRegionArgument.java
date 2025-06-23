package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;

public record SphereRegionArgument(double radius, EnhancedPosArgument center) implements RegionArgument<SphereRegion> {
  public static final MapCodec<SphereRegionArgument> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.DOUBLE.fieldOf("radius").forGetter(SphereRegionArgument::radius),
      EnhancedPosArgument.CODEC.fieldOf("center").forGetter(SphereRegionArgument::center)
  ).apply(i, SphereRegionArgument::new));

  @Override
  public SphereRegion toAbsoluteRegion(ServerCommandSource source) throws CommandSyntaxException {
    return new SphereRegion(radius, center.toAbsolutePos(source));
  }

  @Override
  public @NotNull RegionType<SphereRegion> getType() {
    return RegionTypes.SPHERE;
  }

  @Override
  public @NotNull String asString() {
    return "sphere(" + radius + ", " + center.asString() + ")";
  }
}
