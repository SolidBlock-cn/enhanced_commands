package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.RotationProvider;
import pers.solid.ecmd.util.TextUtil;

import java.util.*;

/**
 * 类似于 {@link TeleportCommand}，但是传送时是按照各实体的位置和方块传送的。
 */
public enum TpRelCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final EnhancedPosArgument posType = new EnhancedPosArgument(EnhancedPosArgument.NumberType.PREFER_DOUBLE, EnhancedPosArgument.IntAlignType.HORIZONTALLY_CENTERED);
    dispatcher.register(ModCommands.literalR2("tprel")
        .requires(source -> source.hasPermission(2))
        .then(Commands.argument("location", posType)
            .executes(context -> execute(
                    context.getSource(),
                    Collections.singleton(context.getSource().getEntityOrException()),
                    context.getSource().getLevel(),
                    EnhancedPosArgument.getPosArgument(context, "location"),
                    new RotationProvider(0, 0, false, false),
                    null
                )
            )
        )
        .then(Commands.argument("targets", EntityArgument.entities())
            .then(Commands.argument("location", posType)
                .executes(context -> execute(
                        context.getSource(),
                        EntityArgument.getEntities(context, "targets"),
                        context.getSource().getLevel(),
                        EnhancedPosArgument.getPosArgument(context, "location"),
                        null,
                        null
                    )
                )
                .then(Commands.argument("rotation", RotationArgument.rotation())
                    .executes(context -> execute(
                            context.getSource(),
                            EntityArgument.getEntities(context, "targets"),
                            context.getSource().getLevel(),
                            EnhancedPosArgument.getPosArgument(context, "location"),
                            RotationArgument.getRotation(context, "rotation"),
                            null
                        )
                    )
                )
                .then(Commands.literal("facing")
                    .then(Commands.literal("entity")
                        .then(Commands.argument("facingEntity", EntityArgument.entity())
                            .executes(context -> execute(
                                    context.getSource(),
                                    EntityArgument.getEntities(context, "targets"),
                                    context.getSource().getLevel(),
                                    EnhancedPosArgument.getPosArgument(context, "location"),
                                    null,
                                    new LookTarget(EntityArgument.getEntity(context, "facingEntity"), EntityAnchorArgument.Anchor.FEET)
                                )
                            )
                            .then(Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                .executes(context -> execute(
                                        context.getSource(),
                                        EntityArgument.getEntities(context, "targets"),
                                        context.getSource().getLevel(),
                                        EnhancedPosArgument.getPosArgument(context, "location"),
                                        null,
                                        new LookTarget(
                                            EntityArgument.getEntity(context, "facingEntity"), EntityAnchorArgument.getAnchor(context, "facingAnchor")
                                        )
                                    )
                                )
                            )
                        )
                    )
                    .then(Commands.argument("facingLocation", posType)
                        .executes(context -> execute(
                                context.getSource(),
                                EntityArgument.getEntities(context, "targets"),
                                context.getSource().getLevel(),
                                EnhancedPosArgument.getPosArgument(context, "location"),
                                null,
                                new LookTarget(EnhancedPosArgument.getPosArgument(context, "facingLocation").getPosition(context.getSource()))
                            )
                        )
                    )
                )
            )
        )
    );
  }

  private static int execute(
      CommandSourceStack source,
      Collection<? extends Entity> targets,
      ServerLevel world,
      Coordinates location,
      @Nullable Coordinates rotation,
      @Nullable LookTarget facingLocation
  ) throws CommandSyntaxException {
    Vec3 vec3d = null;
    Vec2 vec2f;
    Set<RelativeMovement> set = EnumSet.noneOf(RelativeMovement.class);
    if (location.isXRelative()) {
      set.add(RelativeMovement.X);
    }
    if (location.isYRelative()) {
      set.add(RelativeMovement.Y);
    }
    if (location.isZRelative()) {
      set.add(RelativeMovement.Z);
    }
    if (rotation == null) {
      set.add(RelativeMovement.X_ROT);
      set.add(RelativeMovement.Y_ROT);
    } else {
      if (rotation.isXRelative()) {
        set.add(RelativeMovement.X_ROT);
      }
      if (rotation.isYRelative()) {
        set.add(RelativeMovement.Y_ROT);
      }
    }
    for (Entity entity : targets) {
      final CommandSourceStack modifiedSource = entity.createCommandSourceStack();
      vec3d = location.getPosition(modifiedSource);
      vec2f = rotation == null ? null : rotation.getRotation(modifiedSource);
      if (rotation == null) {
        teleport(modifiedSource, entity, world, vec3d.x, vec3d.y, vec3d.z, set, entity.getYRot(), entity.getXRot(), facingLocation);
      } else {
        teleport(modifiedSource, entity, world, vec3d.x, vec3d.y, vec3d.z, set, vec2f.y, vec2f.x, facingLocation);
      }
    }

    if (targets.size() == 1) {
      Vec3 finalVec3d = vec3d;
      source.sendFeedback$ecBridge(() -> Component.translatable(
          "commands.teleport.success.location.single",
          targets.iterator().next().getDisplayName(),
          formatFloat(finalVec3d.x),
          formatFloat(finalVec3d.y),
          formatFloat(finalVec3d.z)
      ), true);
    } else if (!targets.isEmpty()) {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tprel.multiple", targets.size()), true);
    }

    return targets.size();
  }

  private static String formatFloat(double d) {
    return String.format(Locale.ROOT, "%f", d);
  }

  private static void teleport(
      CommandSourceStack source,
      Entity target,
      ServerLevel world,
      double x,
      double y,
      double z,
      Set<RelativeMovement> movementFlags,
      float yaw,
      float pitch,
      @Nullable LookTarget facingLocation
  ) throws CommandSyntaxException {
    BlockPos blockPos = BlockPos.containing(x, y, z);
    if (!Level.isInSpawnableBounds(blockPos)) {
      throw EnhancedPosArgument.OUT_OF_BOUNDS_EXCEPTION.create(TextUtil.wrapVector(blockPos));
    } else {
      float f = Mth.wrapDegrees(yaw);
      float g = Mth.wrapDegrees(pitch);
      if (target.teleportTo(world, x, y, z, movementFlags, f, g)) {
        if (facingLocation != null) {
          facingLocation.look(source, target);
        }

        if (!(target instanceof LivingEntity livingEntity) || !livingEntity.isFallFlying()) {
          target.setDeltaMovement(target.getDeltaMovement().multiply(1.0, 0.0, 1.0));
          target.setOnGround(true);
        }

        if (target instanceof PathfinderMob pathAwareEntity) {
          pathAwareEntity.getNavigation().stop();
        }
      }
    }
  }

  static class LookTarget {
    private final Vec3 targetPos;
    private final Entity target;
    private final EntityAnchorArgument.Anchor targetAnchor;

    public LookTarget(Entity target, EntityAnchorArgument.Anchor targetAnchor) {
      this.target = target;
      this.targetAnchor = targetAnchor;
      this.targetPos = targetAnchor.apply(target);
    }

    public LookTarget(Vec3 targetPos) {
      this.target = null;
      this.targetPos = targetPos;
      this.targetAnchor = null;
    }

    public void look(CommandSourceStack source, Entity entity) {
      if (this.target != null) {
        if (entity instanceof ServerPlayer) {
          ((ServerPlayer) entity).lookAt(source.getAnchor(), this.target, this.targetAnchor);
        } else {
          entity.lookAt(source.getAnchor(), this.targetPos);
        }
      } else {
        entity.lookAt(source.getAnchor(), this.targetPos);
      }
    }
  }
}
