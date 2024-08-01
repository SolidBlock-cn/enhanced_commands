package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.command.argument.EntityArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum TameCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literal("tame")
        .then(argument("targets", entities())
            .executes(context -> executeTrust(context, context.getSource().getPlayerOrThrow()))
            .then(literal("trust")
                .then(argument("owner", player())
                    .executes(context -> executeTrust(context, getPlayer(context, "owner")))))
            .then(literal("mistrust")
                .executes(TameCommand::executeMistrust))
            .then(literal("get")
                .executes(TameCommand::getOwner))));
  }

  public static final DynamicCommandExceptionType NOT_TAMEABLE_SINGLE = new DynamicCommandExceptionType(s -> Text.translatable("enhanced_commands.commands.tame.not_tameable.single", s));
  public static final DynamicCommandExceptionType NOT_TAMEABLE_MULTIPLE = new DynamicCommandExceptionType(s -> Text.translatable("enhanced_commands.commands.tame.not_tameable.multiple", s).enhanced$$());

  public static int executeTrust(CommandContext<ServerCommandSource> context, ServerPlayerEntity owner) throws CommandSyntaxException {
    final Collection<? extends Entity> targets = getEntities(context, "targets");
    if (targets.size() == 1) {
      final Entity entity = targets.iterator().next();
      if (setTrust(entity, owner)) {
        context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.trust.success.single", entity.getDisplayName(), owner.getDisplayName()), true);
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
        context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.trust.success.multiple", finalSuccess, owner.getDisplayName()), true);
        return finalSuccess;
      } else {
        throw NOT_TAMEABLE_MULTIPLE.create(targets.size());
      }
    }
  }

  public static int executeMistrust(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final Collection<? extends Entity> targets = getEntities(context, "targets");
    if (targets.size() == 1) {
      final Entity entity = targets.iterator().next();
      if (setMistrust(entity)) {
        context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.mistrust.success.single", entity.getDisplayName()), true);
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
        context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.mistrust.success.multiple", finalSuccess), true);
        return finalSuccess;
      } else {
        throw NOT_TAMEABLE_MULTIPLE.create(targets.size());
      }
    }
  }

  private static boolean setTrust(Entity entity, ServerPlayerEntity owner) {
    if (entity instanceof AbstractHorseEntity horse) {
      horse.setTame(true);
      horse.setOwnerUuid(owner.getUuid());
      return true;
    } else if (entity instanceof TameableEntity tameable) {
      tameable.setTamed(true, true);
      tameable.setOwnerUuid(owner.getUuid());
      return true;
    } else {
      return false;
    }
  }

  private static boolean setMistrust(Entity entity) {
    if (entity instanceof AbstractHorseEntity horse) {
      horse.setTame(false);
      horse.setOwnerUuid(null);
      return true;
    } else if (entity instanceof TameableEntity tameable) {
      tameable.setTamed(false, true);
      tameable.setOwnerUuid(null);
      return true;
    } else {
      return false;
    }
  }

  public static int getOwner(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final Collection<? extends Entity> targets = getEntities(context, "targets");
    if (targets.size() == 1) {
      final Entity entity = targets.iterator().next();
      if (entity instanceof Tameable tameable) {
        final UUID ownerUuid = tameable.getOwnerUuid();
        if (ownerUuid == null) {
          context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.get.single.not_tamed", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)), false);
          return 0;
        } else {
          final LivingEntity owner = tameable.getOwner();
          if (owner != null) {
            context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.get.single.tamed", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.styled(owner.getDisplayName(), Styles.RESULT)), false);
          } else {
            context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.get.single.tamed", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), Text.literal(ownerUuid.toString()).styled(Styles.RESULT)), false);
          }
          return 1;
        }
      } else {
        context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.get.single.not_tameable", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)), false);
        return 0;
      }
    } else {
      Set<Either<UUID, LivingEntity>> owners = new HashSet<>();
      for (Entity target : targets) {
        if (target instanceof Tameable tameable) {
          final UUID ownerUuid = tameable.getOwnerUuid();
          final LivingEntity owner = tameable.getOwner();
          if (ownerUuid != null) {
            owners.add(owner != null ? Either.right(owner) : Either.left(ownerUuid));
          }
        }
      }
      if (owners.isEmpty()) {
        context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.get.multiple.not_tamed", TextUtil.literal(targets.size()).styled(Styles.TARGET)).enhanced$$(), false);
      } else if (owners.size() == 1) {
        final Either<UUID, LivingEntity> owner = owners.iterator().next();
        @NotNull ServerCommandSource source = context.getSource();
        source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.get.multiple.tamed_by_single", TextUtil.literal(targets.size()).styled(Styles.TARGET), owner.map(uuid -> Text.literal(uuid.toString()).styled(Styles.RESULT), livingEntity -> TextUtil.styled(livingEntity.getDisplayName(), Styles.RESULT))).enhanced$$(), false);
      } else {
        context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.tame.get.multiple.tamed_by_multiple", TextUtil.literal(targets.size()).styled(Styles.TARGET), TextUtil.literal(owners.size()).styled(Styles.RESULT)), false);
      }
      return owners.size();
    }
  }
}
