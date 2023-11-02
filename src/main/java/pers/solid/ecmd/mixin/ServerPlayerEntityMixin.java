package pers.solid.ecmd.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionselection.RegionSelectionType;
import pers.solid.ecmd.regionselection.RegionSelectionTypes;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityExtension {
  @Unique
  private @Nullable Region ec$activeRegion;
  @Unique
  private RegionSelectionType ec$regionSelectionType = RegionSelectionTypes.CUBOID;

  @Override
  public @Nullable Region ec$getActiveRegion() {
    return ec$activeRegion;
  }

  @Override
  public void ec$setActiveRegion(Region region) {
    ec$activeRegion = region;
  }

  @Inject(method = "copyFrom", at = @At("TAIL"))
  public void injectedCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
    // 玩家重生时，需保留这些信息。
    ec$setActiveRegion(((ServerPlayerEntityExtension) oldPlayer).ec$getActiveRegion());
    ec$setRegionSelectionType(((ServerPlayerEntityExtension) oldPlayer).ec$getRegionSelectionType());
  }

  @Override
  public RegionSelectionType ec$getRegionSelectionType() {
    return this.ec$regionSelectionType;
  }

  @Override
  public void ec$setRegionSelectionType(RegionSelectionType regionSelectionType) {
    this.ec$regionSelectionType = regionSelectionType;
  }
}
