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
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.extension.CommandSourceStackExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 此 mixin 用于让 {@link CommandSourceStack} 实现 {@link CommandSourceStackExtension}。
 */
@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackExtensionImpl implements CommandSourceStackExtension, PositionProvider {
  @Unique
  private @Nullable Map<String, Object> enhanced_commands$extraArguments = null;

  @Shadow
  public abstract void sendSuccess(Supplier<Component> messageSupplier, boolean allowLogging);

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
  public abstract ServerPlayer getPlayerOrException();

  @Shadow
  public abstract ServerLevel getLevel();

  @Shadow
  public abstract Entity getEntityOrException();

  @Override
  public final void sendFeedback$ecBridge(Supplier<Component> feedbackSupplier, boolean broadcastToOps) {
    sendSuccess(feedbackSupplier, broadcastToOps);
  }

  @Override
  public Map<String, Object> getExtraArguments$ec() {
    return Objects.requireNonNullElseGet(enhanced_commands$extraArguments, Map::of);
  }

  @Override
  public void addExtraArgument$ec(String name, Object argument) {
    if (enhanced_commands$extraArguments == null) {
      enhanced_commands$extraArguments = new HashMap<>();
    }
    enhanced_commands$extraArguments.put(name, argument);
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
  public Entity getEntityOrThrow$ec() throws CommandSyntaxException {
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
  public Player getPlayerOrThrow$ec() throws CommandSyntaxException {
    return getPlayerOrException();
  }

  @Override
  public ServerLevel getWorld$ec() {
    return getLevel();
  }
}
