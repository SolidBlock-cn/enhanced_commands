package pers.solid.ecmd.mixins.mixin;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.ModTrackedData;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.history.History;
import pers.solid.ecmd.mixins.ext.ServerPlayerEntityExtension;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.regionselection.RegionSelectionType;
import pers.solid.ecmd.regionselection.RegionSelectionTypes;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityExtension, HistoryHolder {
  @Shadow
  @Final
  public MinecraftServer server;
  @Unique
  private final Deque<History> undoableHistories = new ArrayDeque<>();
  @Unique
  private final Deque<History> redoableHistories = new ArrayDeque<>();
  @Unique
  private RegionSelectionType ec$regionSelectionType = RegionSelectionTypes.CUBOID;

  @Override
  public @Nullable RegionSelection getActiveRegion$ec() {
    return ((PlayerEntity) (Object) this).getDataTracker().get(ModTrackedData.PLAYER_REGION_SELECTION).orElse(null);
  }

  @Override
  public void syncActiveRegion$ec() {
    final DataTracker dataTracker = ((PlayerEntity) (Object) this).getDataTracker();
    dataTracker.set(ModTrackedData.PLAYER_REGION_SELECTION, dataTracker.get(ModTrackedData.PLAYER_REGION_SELECTION), true);
  }

  @Inject(method = "copyFrom", at = @At("TAIL"))
  public void injectedCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
    // 玩家重生时，需保留这些信息。
    setActiveRegion$ec(oldPlayer.getActiveRegion$ec());
    setRegionSelectionType$ec(oldPlayer.getRegionSelectionType$ec());
  }

  @Override
  public RegionSelectionType getRegionSelectionType$ec() {
    return this.ec$regionSelectionType;
  }

  @Override
  public void setRegionSelectionType$ec(RegionSelectionType regionSelectionType) {
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
