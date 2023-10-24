package pers.solid.ecmd.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.regionbuilder.RegionBuilderType;
import pers.solid.ecmd.regionbuilder.RegionBuilderTypes;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityExtension {
  @Unique
  private @Nullable RegionArgument<?> ec$activeRegion;
  @Unique
  private @Nullable RegionBuilder ec$regionBuilder;
  @Unique
  private RegionBuilderType ec$regionBuilderType = RegionBuilderTypes.CUBOID;

  @Override
  public @Nullable RegionArgument<?> ec$getActiveRegion() {
    return ec$activeRegion;
  }

  @Override
  public void ec$setActiveRegion(RegionArgument<?> regionArgument) {
    ec$activeRegion = regionArgument;
  }

  @Inject(method = "copyFrom", at = @At("TAIL"))
  public void injectedCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
    // 玩家重生时，需保留这些信息。
    ec$setActiveRegion(((ServerPlayerEntityExtension) oldPlayer).ec$getActiveRegion());
    ec$setRegionBuilder(((ServerPlayerEntityExtension) oldPlayer).ec$getRegionBuilder());
    ec$setRegionBuilderType(((ServerPlayerEntityExtension) oldPlayer).ec$getRegionBuilderType());
  }

  @Override
  public @Nullable RegionBuilder ec$getRegionBuilder() {
    return ec$regionBuilder;
  }

  @Override
  public void ec$setRegionBuilder(RegionBuilder regionBuilder) {
    this.ec$regionBuilder = regionBuilder;
  }

  @Override
  public RegionBuilderType ec$getRegionBuilderType() {
    return this.ec$regionBuilderType;
  }

  @Override
  public void ec$setRegionBuilderType(RegionBuilderType regionBuilderType) {
    this.ec$regionBuilderType = regionBuilderType;
  }
}
