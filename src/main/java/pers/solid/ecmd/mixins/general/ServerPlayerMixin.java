package pers.solid.ecmd.mixins.general;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.history.History;
import pers.solid.ecmd.regionselection.RegionSelectionType;
import pers.solid.ecmd.regionselection.RegionSelectionTypes;
import pers.solid.ecmd.util.extension.HistoryHolder;
import pers.solid.ecmd.util.extension.ServerPlayerExtension;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements ServerPlayerExtension, HistoryHolder {
  @Shadow
  @Final
  public MinecraftServer server;
  @Unique
  private final Deque<History> undoableHistories = new ArrayDeque<>();
  @Unique
  private final Deque<History> redoableHistories = new ArrayDeque<>();
  @Unique
  private RegionSelectionType ec$regionSelectionType = RegionSelectionTypes.CUBOID;


  @Inject(method = "restoreFrom", at = @At("TAIL"))
  public void injectedCopyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
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
