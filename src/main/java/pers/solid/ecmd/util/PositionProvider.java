package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.EntityAnchorAccessor;

public interface PositionProvider {
  PositionProvider EMPTY = of(Vec3.ZERO, Vec2.ZERO, null, EntityAnchorArgument.Anchor.FEET);

  default Vec3 getPosition$ec() {
    throw new NotImplementedException();
  }

  default Vec2 getRotation$ec() {
    throw new NotImplementedException();
  }

  default @Nullable Entity getEntity$ec() {
    throw new NotImplementedException();
  }

  default Entity getEntityOrThrow$ec() throws CommandSyntaxException {
    final Entity entity = getEntity$ec();
    if (entity == null) {
      throw CommandSourceStack.ERROR_NOT_ENTITY.create();
    } else {
      return entity;
    }
  }

  default @Nullable Player getPlayer$ec() {
    final Entity entity = getEntity$ec();
    return entity instanceof Player player ? player : null;
  }

  default Player getPlayerOrThrow$ec() throws CommandSyntaxException {
    final Player playerEntity = getPlayer$ec();
    if (playerEntity == null) {
      throw CommandSourceStack.ERROR_NOT_PLAYER.create();
    } else {
      return playerEntity;
    }
  }

  default EntityAnchorArgument.Anchor getEntityAnchor$ec() {
    throw new NotImplementedException();
  }

  default Vec3 getPositionAt$ec(PositionProvider positionProvider) {
    Entity entity = positionProvider.getEntity$ec();
    return entity == null ? positionProvider.getPosition$ec() : ((EntityAnchorAccessor) (Enum<EntityAnchorArgument.Anchor>) getEntityAnchor$ec()).getTransform().apply(positionProvider.getPosition$ec(), entity);
  }

  default Level getWorld$ec() {
    throw new NotImplementedException();
  }

  static PositionProvider of(Vec3 position, Vec2 rotation, @Nullable Player player, EntityAnchorArgument.Anchor entityAnchor) {
    return new Simple(position, rotation, player, entityAnchor);
  }

  static PositionProvider of(Entity entity) {
    if (entity.level() instanceof ServerLevel) {
      return entity.createCommandSourceStack();
    } else {
      return of(entity.position(), entity.getRotationVector(), null, EntityAnchorArgument.Anchor.FEET);
    }
  }

  record Simple(Vec3 getPosition$ec, Vec2 getRotation$ec, @Nullable Player getEntity$ec, EntityAnchorArgument.Anchor getEntityAnchor$ec) implements PositionProvider {
  }
}
