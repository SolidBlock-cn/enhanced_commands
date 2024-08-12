package pers.solid.ecmd.mixins.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.history.History;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionselection.RegionSelectionType;
import pers.solid.ecmd.regionselection.RegionSelectionTypes;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityExtension, HistoryHolder {
  @Unique
  private final Deque<History> undoableHistories = new ArrayDeque<>();
  @Unique
  private final Deque<History> redoableHistories = new ArrayDeque<>();
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

  @Override
  public Deque<History> getUndoableHistories$ec() {
    return undoableHistories;
  }

  @Override
  public Deque<History> getRedoableHistories$ec() {
    return redoableHistories;
  }
}
