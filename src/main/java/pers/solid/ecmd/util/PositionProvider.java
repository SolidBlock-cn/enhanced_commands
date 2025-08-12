package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.EntityAnchorAccessor;

public interface PositionProvider {
  PositionProvider EMPTY = of(Vec3d.ZERO, Vec2f.ZERO, null, EntityAnchorArgumentType.EntityAnchor.FEET);

  default Vec3d getPosition$ec() {
    throw new NotImplementedException();
  }

  default Vec2f getRotation$ec() {
    throw new NotImplementedException();
  }

  default @Nullable Entity getEntity$ec() {
    throw new NotImplementedException();
  }

  default @NotNull Entity getEntityOrThrow$ec() throws CommandSyntaxException {
    final Entity entity = getEntity$ec();
    if (entity == null) {
      throw ServerCommandSource.REQUIRES_ENTITY_EXCEPTION.create();
    } else {
      return entity;
    }
  }

  default @Nullable PlayerEntity getPlayer$ec() {
    final Entity entity = getEntity$ec();
    return entity instanceof PlayerEntity player ? player : null;
  }

  default @NotNull PlayerEntity getPlayerOrThrow$ec() throws CommandSyntaxException {
    final PlayerEntity playerEntity = getPlayer$ec();
    if (playerEntity == null) {
      throw ServerCommandSource.REQUIRES_PLAYER_EXCEPTION.create();
    } else {
      return playerEntity;
    }
  }

  default EntityAnchorArgumentType.EntityAnchor getEntityAnchor$ec() {
    throw new NotImplementedException();
  }

  default Vec3d getPositionAt$ec(PositionProvider positionProvider) {
    Entity entity = positionProvider.getEntity$ec();
    return entity == null ? positionProvider.getPosition$ec() : ((EntityAnchorAccessor) (Enum<EntityAnchorArgumentType.EntityAnchor>) getEntityAnchor$ec()).getOffset().apply(positionProvider.getPosition$ec(), entity);
  }

  default World getWorld$ec() {
    throw new NotImplementedException();
  }

  static PositionProvider of(Vec3d position, Vec2f rotation, @Nullable PlayerEntity player, EntityAnchorArgumentType.EntityAnchor entityAnchor) {
    return new Simple(position, rotation, player, entityAnchor);
  }

  record Simple(Vec3d getPosition$ec, Vec2f getRotation$ec, @Nullable PlayerEntity getEntity$ec, EntityAnchorArgumentType.EntityAnchor getEntityAnchor$ec) implements PositionProvider {
  }
}
