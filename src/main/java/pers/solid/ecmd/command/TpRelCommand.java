package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;

import java.util.*;

/**
 * 类似于 {@link TeleportCommand}，但是传送时是按照各实体的位置和方块传送的。
 */
public enum TpRelCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final EnhancedPosArgumentType posType = new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.DOUBLE_OR_CENTERED_INT, false);
    dispatcher.register(CommandManager.literal("tprel")
        .requires(source -> source.hasPermissionLevel(2))
        .then(CommandManager.argument("location", posType)
            .executes(context -> execute(
                    context.getSource(),
                    Collections.singleton(context.getSource().getEntityOrThrow()),
                    context.getSource().getWorld(),
                    EnhancedPosArgumentType.getPosArgument(context, "location"),
                    DefaultPosArgument.zero(),
                    null
                )
            )
        )
        .then(CommandManager.argument("targets", EntityArgumentType.entities())
            .then(CommandManager.argument("location", posType)
                .executes(context -> execute(
                        context.getSource(),
                        EntityArgumentType.getEntities(context, "targets"),
                        context.getSource().getWorld(),
                        EnhancedPosArgumentType.getPosArgument(context, "location"),
                        null,
                        null
                    )
                )
                .then(CommandManager.argument("rotation", RotationArgumentType.rotation())
                    .executes(context -> execute(
                            context.getSource(),
                            EntityArgumentType.getEntities(context, "targets"),
                            context.getSource().getWorld(),
                            EnhancedPosArgumentType.getPosArgument(context, "location"),
                            RotationArgumentType.getRotation(context, "rotation"),
                            null
                        )
                    )
                )
                .then(CommandManager.literal("facing")
                    .then(CommandManager.literal("entity")
                        .then(CommandManager.argument("facingEntity", EntityArgumentType.entity())
                            .executes(context -> execute(
                                    context.getSource(),
                                    EntityArgumentType.getEntities(context, "targets"),
                                    context.getSource().getWorld(),
                                    EnhancedPosArgumentType.getPosArgument(context, "location"),
                                    null,
                                    new LookTarget(EntityArgumentType.getEntity(context, "facingEntity"), EntityAnchorArgumentType.EntityAnchor.FEET)
                                )
                            )
                            .then(CommandManager.argument("facingAnchor", EntityAnchorArgumentType.entityAnchor())
                                .executes(context -> execute(
                                        context.getSource(),
                                        EntityArgumentType.getEntities(context, "targets"),
                                        context.getSource().getWorld(),
                                        EnhancedPosArgumentType.getPosArgument(context, "location"),
                                        null,
                                        new LookTarget(
                                            EntityArgumentType.getEntity(context, "facingEntity"), EntityAnchorArgumentType.getEntityAnchor(context, "facingAnchor")
                                        )
                                    )
                                )
                            )
                        )
                    )
                    .then(CommandManager.argument("facingLocation", posType)
                        .executes(context -> execute(
                                context.getSource(),
                                EntityArgumentType.getEntities(context, "targets"),
                                context.getSource().getWorld(),
                                EnhancedPosArgumentType.getPosArgument(context, "location"),
                                null,
                                new LookTarget(EnhancedPosArgumentType.getPosArgument(context, "facingLocation").toAbsolutePos(context.getSource()))
                            )
                        )
                    )
                )
            )
        )
    );
  }

  private static int execute(
      ServerCommandSource source,
      Collection<? extends Entity> targets,
      ServerWorld world,
      PosArgument location,
      @Nullable PosArgument rotation,
      @Nullable LookTarget facingLocation
  ) throws CommandSyntaxException {
    Vec3d vec3d = null;
    Vec2f vec2f;
    Set<PositionFlag> set = EnumSet.noneOf(PositionFlag.class);
    if (location.isXRelative()) {set.add(PositionFlag.X);}
    if (location.isYRelative()) {set.add(PositionFlag.Y);}
    if (location.isZRelative()) {set.add(PositionFlag.Z);}
    if (rotation == null) {
      set.add(PositionFlag.X_ROT);
      set.add(PositionFlag.Y_ROT);
    } else {
      if (rotation.isXRelative()) {
        set.add(PositionFlag.X_ROT);
      }
      if (rotation.isYRelative()) {
        set.add(PositionFlag.Y_ROT);
      }
    }
    for (Entity entity : targets) {
      final ServerCommandSource modifiedSource = source.withEntity(entity).withPosition(entity.getPos()).withRotation(entity.getRotationClient()).withWorld((ServerWorld) entity.getWorld());
      vec3d = location.toAbsolutePos(modifiedSource);
      vec2f = rotation == null ? null : rotation.toAbsoluteRotation(modifiedSource);
      if (rotation == null) {
        teleport(modifiedSource, entity, world, vec3d.x, vec3d.y, vec3d.z, set, entity.getYaw(), entity.getPitch(), facingLocation);
      } else {
        teleport(modifiedSource, entity, world, vec3d.x, vec3d.y, vec3d.z, set, vec2f.y, vec2f.x, facingLocation);
      }
    }

    if (targets.size() == 1) {
      source.sendFeedback(Text.translatable(
              "commands.teleport.success.location.single",
              targets.iterator().next().getDisplayName(),
              formatFloat(vec3d.x),
              formatFloat(vec3d.y),
              formatFloat(vec3d.z)
          ),
          true
      );
    } else if (!targets.isEmpty()) {
      source.sendFeedback(Text.translatable("commands.teleport.success.location.multiple", targets.size(), formatFloat(vec3d.x), formatFloat(vec3d.y), formatFloat(vec3d.z)), true
      );
    }

    return targets.size();
  }

  private static String formatFloat(double d) {
    return String.format(Locale.ROOT, "%f", d);
  }

  private static void teleport(
      ServerCommandSource source,
      Entity target,
      ServerWorld world,
      double x,
      double y,
      double z,
      Set<PositionFlag> movementFlags,
      float yaw,
      float pitch,
      @Nullable LookTarget facingLocation
  ) throws CommandSyntaxException {
    BlockPos blockPos = BlockPos.ofFloored(x, y, z);
    if (!World.isValid(blockPos)) {
      throw EnhancedPosArgumentType.OUT_OF_BOUNDS_EXCEPTION.create(blockPos);
    } else {
      float f = MathHelper.wrapDegrees(yaw);
      float g = MathHelper.wrapDegrees(pitch);
      if (target.teleport(world, x, y, z, movementFlags, f, g)) {
        if (facingLocation != null) {
          facingLocation.look(source, target);
        }

        if (!(target instanceof LivingEntity livingEntity) || !livingEntity.isFallFlying()) {
          target.setVelocity(target.getVelocity().multiply(1.0, 0.0, 1.0));
          target.setOnGround(true);
        }

        if (target instanceof PathAwareEntity pathAwareEntity) {
          pathAwareEntity.getNavigation().stop();
        }
      }
    }
  }

  static class LookTarget {
    private final Vec3d targetPos;
    private final Entity target;
    private final EntityAnchorArgumentType.EntityAnchor targetAnchor;

    public LookTarget(Entity target, EntityAnchorArgumentType.EntityAnchor targetAnchor) {
      this.target = target;
      this.targetAnchor = targetAnchor;
      this.targetPos = targetAnchor.positionAt(target);
    }

    public LookTarget(Vec3d targetPos) {
      this.target = null;
      this.targetPos = targetPos;
      this.targetAnchor = null;
    }

    public void look(ServerCommandSource source, Entity entity) {
      if (this.target != null) {
        if (entity instanceof ServerPlayerEntity) {
          ((ServerPlayerEntity) entity).lookAtEntity(source.getEntityAnchor(), this.target, this.targetAnchor);
        } else {
          entity.lookAt(source.getEntityAnchor(), this.targetPos);
        }
      } else {
        entity.lookAt(source.getEntityAnchor(), this.targetPos);
      }
    }
  }
}
