package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.EntityAnchorAccessor;

public interface PositionProvider {
  PositionProvider EMPTY = of(Vec3d.ZERO, Vec2f.ZERO, null, EntityAnchorArgumentType.EntityAnchor.FEET);

  default Vec3d position$ec() {
    throw new NotImplementedException();
  }

  default Vec2f rotation$ec() {
    throw new NotImplementedException();
  }

  default @Nullable Entity entity$ec() {
    throw new NotImplementedException();
  }

  default @Nullable PlayerEntity player$ec() {
    final Entity entity = entity$ec();
    return entity instanceof PlayerEntity player ? player : null;
  }

  default @NotNull PlayerEntity playerOrThrow$ec() throws CommandSyntaxException {
    final PlayerEntity playerEntity = player$ec();
    if (playerEntity == null) {
      throw ServerCommandSource.REQUIRES_PLAYER_EXCEPTION.create();
    } else {
      return playerEntity;
    }
  }

  default EntityAnchorArgumentType.EntityAnchor entityAnchor$ec() {
    throw new NotImplementedException();
  }

  default Vec3d positionAt$ec(PositionProvider positionProvider) {
    Entity entity = positionProvider.entity$ec();
    return entity == null ? positionProvider.position$ec() : ((EntityAnchorAccessor) (Enum<EntityAnchorArgumentType.EntityAnchor>) entityAnchor$ec()).getOffset().apply(positionProvider.position$ec(), entity);
  }

  static PositionProvider of(Vec3d position, Vec2f rotation, @Nullable PlayerEntity player, EntityAnchorArgumentType.EntityAnchor entityAnchor) {
    return new Simple(position, rotation, player, entityAnchor);
  }

  record Simple(Vec3d position$ec, Vec2f rotation$ec, @Nullable PlayerEntity entity$ec, EntityAnchorArgumentType.EntityAnchor entityAnchor$ec) implements PositionProvider {
  }
}
