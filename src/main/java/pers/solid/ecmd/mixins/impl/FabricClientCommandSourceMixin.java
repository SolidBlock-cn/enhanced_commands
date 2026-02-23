package pers.solid.ecmd.mixins.impl;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pers.solid.ecmd.util.PositionProvider;

@Mixin(FabricClientCommandSource.class)
public interface FabricClientCommandSourceMixin extends PositionProvider {
  @Shadow
  Vec3 getPosition();

  @Shadow
  Vec2 getRotation();

  @Shadow
  LocalPlayer getPlayer();

  @Shadow
  ClientLevel getWorld();

  @Override
  default Vec3 getPosition$ec() {
    return getPosition();
  }

  @Override
  default Vec2 getRotation$ec() {
    return getRotation();
  }

  @Override
  default @Nullable Player getEntity$ec() {
    return getPlayer();
  }

  @Override
  @Nullable
  default LocalPlayer getPlayer$ec() {
    return getPlayer();
  }

  @Override
  default EntityAnchorArgument.Anchor getEntityAnchor$ec() {
    return EntityAnchorArgument.Anchor.FEET;
  }

  @Override
  default ClientLevel getWorld$ec() {
    return getWorld();
  }
}
