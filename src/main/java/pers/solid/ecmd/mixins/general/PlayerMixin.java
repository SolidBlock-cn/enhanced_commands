package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.EnhancedCommandsTrackedData;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.extension.PlayerExtension;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerExtension {
  /**
   * 当 ignoreBoundary 设置为 true 时，允许玩家传送到世界界限以外，也就是不要执行 setPosition。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setPos(DDD)V"))
  private boolean noClampPos(Player instance, double x, double y, double z) {
    return !DebugConfig.current.ignoreBoundary;
  }

  /**
   * 将 data tracker 添加到玩家
   */
  @Inject(method = "defineSynchedData", at = @At("TAIL"))
  private void initModDataTracker(SynchedEntityData.Builder builder, CallbackInfo ci) {
    builder.define(EnhancedCommandsTrackedData.PLAYER_REGION_SELECTION, Optional.empty());
  }

  @Override
  public @Nullable RegionSelection getActiveRegion$ec() {
    return ((Entity) (Object) this).getEntityData().get(EnhancedCommandsTrackedData.PLAYER_REGION_SELECTION).orElse(null);
  }

  @Override
  public void setActiveRegion$ec(@Nullable RegionSelection region) {
    ((Entity) (Object) this).getEntityData().set(EnhancedCommandsTrackedData.PLAYER_REGION_SELECTION, Optional.ofNullable(region), true);
  }

  @Override
  public void syncActiveRegion$ec() {
    final SynchedEntityData dataTracker = ((Player) (Object) this).getEntityData();
    dataTracker.set(EnhancedCommandsTrackedData.PLAYER_REGION_SELECTION, dataTracker.get(EnhancedCommandsTrackedData.PLAYER_REGION_SELECTION), true);
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
