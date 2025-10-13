package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.ModTrackedData;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.mixins.ext.PlayerEntityExtension;
import pers.solid.ecmd.regionselection.RegionSelection;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements PlayerEntityExtension {
  /**
   * 当 ignoreBoundary 设置为 true 时，允许玩家传送到世界界限以外，也就是不要执行 setPosition。
   *
   * @see DebugConfig#ignoreBoundary
   */
  @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setPosition(DDD)V"))
  private boolean noClampPos(PlayerEntity instance, double x, double y, double z) {
    return !DebugConfig.current.ignoreBoundary;
  }

  /**
   * 将 data tracker 添加到玩家
   */
  @Inject(method = "initDataTracker", at = @At("TAIL"))
  private void initModDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
    builder.add(ModTrackedData.PLAYER_REGION_SELECTION, Optional.empty());
  }

  @Override
  public @Nullable RegionSelection getActiveRegion$ec() {
    return ((PlayerEntity) (Object) this).getDataTracker().get(ModTrackedData.PLAYER_REGION_SELECTION).orElse(null);
  }

  @Override
  public void setActiveRegion$ec(@Nullable RegionSelection region) {
    ((PlayerEntity) (Object) this).getDataTracker().set(ModTrackedData.PLAYER_REGION_SELECTION, Optional.ofNullable(region));
  }

  @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
  private void readModDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
    final NbtElement value = nbt.get("active_region_ec");
    if (value != null) {
      setActiveRegion$ec(RegionSelection.fromNbt(value));
    } else {
      setActiveRegion$ec(null);
    }
  }

  @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
  private void writeModDataToNbt(NbtCompound nbt, CallbackInfo ci) {
    final RegionSelection activeRegion = getActiveRegion$ec();
    if (activeRegion != null) {
      nbt.put("active_region_ec", activeRegion.createNbt());
    } else {
      nbt.remove("active_region_ec");
    }
  }

}
