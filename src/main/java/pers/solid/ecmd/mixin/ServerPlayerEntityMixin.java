package pers.solid.ecmd.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityExtension {
  @Unique
  private RegionArgument<?> ec_activeRegion;

  @Override
  public @Nullable RegionArgument<?> ec_getActiveRegion() {
    return ec_activeRegion;
  }

  @Override
  public void ec_setActiveRegion(RegionArgument<?> regionArgument) {
    ec_activeRegion = regionArgument;
  }

  @Inject(method = "copyFrom", at = @At("TAIL"))
  public void injectedCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
    // 玩家重生时，需保留这些信息。
    ec_setActiveRegion(((ServerPlayerEntityExtension) oldPlayer).ec_getActiveRegion());
  }
}
