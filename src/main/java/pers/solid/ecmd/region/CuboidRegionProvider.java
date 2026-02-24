package pers.solid.ecmd.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

public interface CuboidRegionProvider<R extends CuboidRegion> extends RegionProvider<R> {
  MapCodec<CuboidRegionProvider<?>> CODEC = Codec.BOOL.dispatchMap("block", r -> r instanceof BlockCuboidRegionProvider, isBlock -> isBlock ? BlockCuboidRegionProvider.CODEC : PreciseCuboidRegionProvider.CODEC);
}
