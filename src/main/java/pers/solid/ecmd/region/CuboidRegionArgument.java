package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

public interface CuboidRegionArgument<R extends CuboidRegion> extends RegionArgument<R> {
  MapCodec<CuboidRegionArgument<?>> CODEC = Codec.BOOL.dispatchMap("block", r -> r instanceof BlockCuboidRegionArgument, isBlock -> isBlock ? BlockCuboidRegionArgument.CODEC : PreciseCuboidRegionArgument.CODEC);
}
