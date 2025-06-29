package pers.solid.ecmd.mixins.impl;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pers.solid.ecmd.util.PositionProvider;

@Mixin(FabricClientCommandSource.class)
public interface FabricClientCommandSourceMixin extends PositionProvider {
  @Shadow
  Vec3d getPosition();

  @Shadow
  Vec2f getRotation();

  @Shadow
  ClientPlayerEntity getPlayer();

  @Override
  default Vec3d position$ec() {
    return getPosition();
  }

  @Override
  default Vec2f rotation$ec() {
    return getRotation();
  }

  @Override
  default @Nullable PlayerEntity entity$ec() {
    return getPlayer();
  }

  @Override
  @Nullable
  default ClientPlayerEntity player$ec() {
    return getPlayer();
  }

  @Override
  default EntityAnchorArgumentType.EntityAnchor entityAnchor$ec() {
    return EntityAnchorArgumentType.EntityAnchor.FEET;
  }
}
