package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.*;

public enum TameCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(literal("tame")
        .then(argument("targets", entities())
            .executes(context -> executeTrust(context, context.getSource().getPlayerOrException()))
            .then(literal("trust")
                .then(argument("owner", player())
                    .executes(context -> executeTrust(context, getPlayer(context, "owner")))))
            .then(literal("mistrust")
                .executes(TameCommand::executeMistrust))
            .then(literal("get")
                .executes(TameCommand::getOwner))));
  }

  public static final DynamicCommandExceptionType NOT_TAMEABLE_SINGLE = new DynamicCommandExceptionType(s -> Component.translatable("enhanced_commands.commands.tame.not_tameable.single", s));
  public static final DynamicCommandExceptionType NOT_TAMEABLE_MULTIPLE = new DynamicCommandExceptionType(s -> Component.translatable("enhanced_commands.commands.tame.not_tameable.multiple", s).enhanced$$());

  public static int executeTrust(CommandContext<CommandSourceStack> context, ServerPlayer owner) throws CommandSyntaxException {
    final Collection<? extends Entity> targets = getEntities(context, "targets");
    if (targets.size() == 1) {
      final Entity entity = targets.iterator().next();
      if (setTrust(entity, owner)) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.trust.success.single", entity.getDisplayName(), owner.getDisplayName()), true);
        return 1;
      } else {
        throw NOT_TAMEABLE_SINGLE.create(entity.getDisplayName());
      }
    } else {
      int success = 0;
      for (Entity target : targets) {
        if (setTrust(target, owner)) {
          success++;
        }
      }
      if (success > 0) {
        int finalSuccess = success;
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.trust.success.multiple", finalSuccess, owner.getDisplayName()), true);
        return finalSuccess;
      } else {
        throw NOT_TAMEABLE_MULTIPLE.create(targets.size());
      }
    }
  }

  public static int executeMistrust(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final Collection<? extends Entity> targets = getEntities(context, "targets");
    if (targets.size() == 1) {
      final Entity entity = targets.iterator().next();
      if (setMistrust(entity)) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.mistrust.success.single", entity.getDisplayName()), true);
        return 1;
      } else {
        throw NOT_TAMEABLE_SINGLE.create(entity.getDisplayName());
      }
    } else {
      int success = 0;
      for (Entity target : targets) {
        if (setMistrust(target)) {
          success++;
        }
      }
      if (success > 0) {
        int finalSuccess = success;
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.mistrust.success.multiple", finalSuccess), true);
        return finalSuccess;
      } else {
        throw NOT_TAMEABLE_MULTIPLE.create(targets.size());
      }
    }
  }

  private static boolean setTrust(Entity entity, ServerPlayer owner) {
    if (entity instanceof AbstractHorse horse) {
      horse.setTamed(true);
      horse.setOwnerUUID(owner.getUUID());
      return true;
    } else if (entity instanceof TamableAnimal tameable) {
      tameable.setTame(true, true);
      tameable.setOwnerUUID(owner.getUUID());
      return true;
    } else {
      return false;
    }
  }

  private static boolean setMistrust(Entity entity) {
    if (entity instanceof AbstractHorse horse) {
      horse.setTamed(false);
      horse.setOwnerUUID(null);
      return true;
    } else if (entity instanceof TamableAnimal tameable) {
      tameable.setTame(false, true);
      tameable.setOwnerUUID(null);
      return true;
    } else {
      return false;
    }
  }

  public static int getOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final Collection<? extends Entity> targets = getEntities(context, "targets");
    if (targets.size() == 1) {
      final Entity entity = targets.iterator().next();
      if (entity instanceof OwnableEntity tameable) {
        final UUID ownerUuid = tameable.getOwnerUUID();
        if (ownerUuid == null) {
          context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.get.single.not_tamed", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)), false);
          return 0;
        } else {
          final LivingEntity owner = tameable.getOwner();
          if (owner != null) {
            context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.get.single.tamed", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.styled(owner.getDisplayName(), Styles.RESULT)), false);
          } else {
            context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.get.single.tamed", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), Component.literal(ownerUuid.toString()).withStyle(Styles.RESULT)), false);
          }
          return 1;
        }
      } else {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.get.single.not_tameable", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)), false);
        return 0;
      }
    } else {
      Set<Either<UUID, LivingEntity>> owners = new HashSet<>();
      for (Entity target : targets) {
        if (target instanceof OwnableEntity tameable) {
          final UUID ownerUuid = tameable.getOwnerUUID();
          final LivingEntity owner = tameable.getOwner();
          if (ownerUuid != null) {
            owners.add(owner != null ? Either.right(owner) : Either.left(ownerUuid));
          }
        }
      }
      if (owners.isEmpty()) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.get.multiple.not_tamed", TextUtil.literal(targets.size()).withStyle(Styles.TARGET)).enhanced$$(), false);
      } else if (owners.size() == 1) {
        final Either<UUID, LivingEntity> owner = owners.iterator().next();
        @NotNull CommandSourceStack source = context.getSource();
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.get.multiple.tamed_by_single", TextUtil.literal(targets.size()).withStyle(Styles.TARGET), owner.map(uuid -> Component.literal(uuid.toString()).withStyle(Styles.RESULT), livingEntity -> TextUtil.styled(livingEntity.getDisplayName(), Styles.RESULT))).enhanced$$(), false);
      } else {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tame.get.multiple.tamed_by_multiple", TextUtil.literal(targets.size()).withStyle(Styles.TARGET), TextUtil.literal(owners.size()).withStyle(Styles.RESULT)), false);
      }
      return owners.size();
    }
  }
}
