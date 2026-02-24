package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.ModTrackedData;
import pers.solid.ecmd.command.DebugIgnoreBoundaryCommand;
import pers.solid.ecmd.mixins.ext.PlayerExtension;
import pers.solid.ecmd.regionselection.RegionSelection;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerExtension {
  /**
   * 当 ignoreBoundary 设置为 true 时，允许玩家传送到世界界限以外，也就是不要执行 setPosition。
   *
   * @see DebugIgnoreBoundaryCommand#ignoreBoundary
   */
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setPos(DDD)V"))
  private boolean noClampPos(Player instance, double x, double y, double z) {
    return !DebugIgnoreBoundaryCommand.ignoreBoundary;
  }

  /**
   * 将 data tracker 添加到玩家
   */
  @Inject(method = "defineSynchedData", at = @At("TAIL"))
  private void initModDataTracker(SynchedEntityData.Builder builder, CallbackInfo ci) {
    builder.define(ModTrackedData.PLAYER_REGION_SELECTION, Optional.empty());
  }

  @Override
  public @Nullable RegionSelection getActiveRegion$ec() {
    return ((Player) (Object) this).getEntityData().get(ModTrackedData.PLAYER_REGION_SELECTION).orElse(null);
  }

  @Override
  public void setActiveRegion$ec(@Nullable RegionSelection region) {
    ((Player) (Object) this).getEntityData().set(ModTrackedData.PLAYER_REGION_SELECTION, Optional.ofNullable(region));
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void readModDataFromNbt(CompoundTag nbt, CallbackInfo ci) {
    final Tag value = nbt.get("active_region_ec");
    if (value != null) {
      setActiveRegion$ec(RegionSelection.fromNbt(value));
    } else {
      setActiveRegion$ec(null);
    }
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void writeModDataToNbt(CompoundTag nbt, CallbackInfo ci) {
    final RegionSelection activeRegion = getActiveRegion$ec();
    if (activeRegion != null) {
      nbt.put("active_region_ec", activeRegion.createNbt());
    } else {
      nbt.remove("active_region_ec");
    }
  }

}
