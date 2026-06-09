package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.EnhancedCommandsTrackedData;
import pers.solid.ecmd.config.DebugConfig;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.extension.PlayerExtension;

import java.util.Optional;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements PlayerExtension {
  @Shadow
  @Final
  private static Logger LOGGER;

  protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
    super(entityType, level);
  }

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
    builder.define(EnhancedCommandsTrackedData.DATA_ACTIVE_REGION_ID, Optional.empty());
  }

  @Override
  public @Nullable RegionSelection getActiveRegion$ec() {
    return this.getEntityData().get(EnhancedCommandsTrackedData.DATA_ACTIVE_REGION_ID).orElse(null);
  }

  @Override
  public void setActiveRegion$ec(@Nullable RegionSelection region) {
    this.getEntityData().set(EnhancedCommandsTrackedData.DATA_ACTIVE_REGION_ID, Optional.ofNullable(region), true);
  }

  @Override
  public void syncActiveRegion$ec() {
    final SynchedEntityData dataTracker = getEntityData();
    dataTracker.set(EnhancedCommandsTrackedData.DATA_ACTIVE_REGION_ID, dataTracker.get(EnhancedCommandsTrackedData.DATA_ACTIVE_REGION_ID), true);
  }

  @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
  private void readModSaveData(CompoundTag tag, CallbackInfo ci) {
    final Tag value = tag.get("enhanced_commands:active_region");
    if (value != null) {
      // 这里的 createSerializationContext 可能也不需要
      setActiveRegion$ec(RegionSelection.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), value).resultOrPartial(LOGGER::error).orElse(null));
    } else {
      setActiveRegion$ec(null);
    }
  }

  @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
  private void writeModSaveData(CompoundTag tag, CallbackInfo ci) {
    final RegionSelection activeRegion = getActiveRegion$ec();
    if (activeRegion != null) {
      tag.put("enhanced_commands:active_region", RegionSelection.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), activeRegion).resultOrPartial(LOGGER::error).orElse(null));
    } else {
      tag.remove("enhanced_commands:active_region");
    }
  }

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z"))
  private boolean alwaysNoPhysics(boolean original) {
    return DebugConfig.current.ghostPlayers || original;
  }
}
