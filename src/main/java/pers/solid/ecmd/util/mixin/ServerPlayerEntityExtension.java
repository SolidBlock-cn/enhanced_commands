package pers.solid.ecmd.util.mixin;

import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.RegionArgument;

public interface ServerPlayerEntityExtension {
  @Nullable RegionArgument<?> ec_getActiveRegion();

  void ec_setActiveRegion(RegionArgument<?> regionArgument);
}
