package pers.solid.ecmd.mixins.impl;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
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
 * 此 mixin 用于让 {@link ServerCommandSource} 实现 {@link ServerCommandSourceExtension}。
 */
@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceExtensionImpl implements ServerCommandSourceExtension, PositionProvider {
  @Unique
  private Map<String, Object> extraArguments = null;

  @Shadow
  public abstract void sendFeedback(Supplier<Text> feedbackSupplier, boolean broadcastToOps);

  @Shadow
  public abstract Vec3d getPosition();

  @Shadow
  public abstract Vec2f getRotation();

  @Shadow
  @Nullable
  public abstract ServerPlayerEntity getPlayer();

  @Shadow
  public abstract EntityAnchorArgumentType.EntityAnchor getEntityAnchor();

  @Shadow
  public abstract ServerPlayerEntity getPlayerOrThrow() throws CommandSyntaxException;

  @Override
  public final void sendFeedback$ecBridge(Supplier<Text> feedbackSupplier, boolean broadcastToOps) {
    sendFeedback(feedbackSupplier, broadcastToOps);
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
  public Vec3d position$ec() {
    return getPosition();
  }

  @Override
  public Vec2f rotation$ec() {
    return getRotation();
  }

  @Override
  public @Nullable PlayerEntity entity$ec() {
    return getPlayer();
  }

  @Override
  public EntityAnchorArgumentType.EntityAnchor entityAnchor$ec() {
    return getEntityAnchor();
  }

  @Override
  public @Nullable PlayerEntity player$ec() {
    return getPlayer();
  }

  @Override
  public @NotNull PlayerEntity playerOrThrow$ec() throws CommandSyntaxException {
    return getPlayerOrThrow();
  }
}
