package pers.solid.ecmd.mixins.impl;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.mixins.ext.ServerCommandSourceExtension;
import pers.solid.ecmd.util.PositionProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 此 mixin 用于让 {@link CommandSourceStack} 实现 {@link ServerCommandSourceExtension}。
 */
@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackExtensionImpl implements ServerCommandSourceExtension, PositionProvider {
  @Unique
  private Map<String, Object> extraArguments = null;

  @Shadow
  public abstract void sendSuccess(Supplier<Component> feedbackSupplier, boolean broadcastToOps);

  @Shadow
  public abstract Vec3 getPosition();

  @Shadow
  public abstract Vec2 getRotation();

  @Shadow
  @Nullable
  public abstract ServerPlayer getPlayer();

  @Shadow
  public abstract EntityAnchorArgument.Anchor getAnchor();

  @Shadow
  public abstract ServerPlayer getPlayerOrException() throws CommandSyntaxException;

  @Shadow
  public abstract ServerLevel getLevel();

  @Shadow
  public abstract Entity getEntityOrException() throws CommandSyntaxException;

  @Override
  public final void sendFeedback$ecBridge(Supplier<Component> feedbackSupplier, boolean broadcastToOps) {
    sendSuccess(feedbackSupplier, broadcastToOps);
  }

  @Override
  public @NotNull Map<String, Object> getExtraArguments$ec() {
    return Objects.requireNonNullElseGet(extraArguments, Map::of);
  }

  @Override
  public void addExtraArgument$ec(String name, Object argument) {
    if (extraArguments == null) {
      extraArguments = new HashMap<>();
    }
    extraArguments.put(name, argument);
  }

  @Override
  public Vec3 getPosition$ec() {
    return getPosition();
  }

  @Override
  public Vec2 getRotation$ec() {
    return getRotation();
  }

  @Override
  public @Nullable Player getEntity$ec() {
    return getPlayer();
  }

  @Override
  public @NotNull Entity getEntityOrThrow$ec() throws CommandSyntaxException {
    return getEntityOrException();
  }

  @Override
  public EntityAnchorArgument.Anchor getEntityAnchor$ec() {
    return getAnchor();
  }

  @Override
  public @Nullable Player getPlayer$ec() {
    return getPlayer();
  }

  @Override
  public @NotNull Player getPlayerOrThrow$ec() throws CommandSyntaxException {
    return getPlayerOrException();
  }

  @Override
  public ServerLevel getWorld$ec() {
    return getLevel();
  }
}
