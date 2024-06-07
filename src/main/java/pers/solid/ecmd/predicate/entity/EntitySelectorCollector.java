package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.StringIdentifiable;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 特殊的实体选择器从世界中收集实体的规则。当实体选择器应用了这些特殊的规则时，将按照这些规则收集实体，而非从世界中收集所有实体。
 */
public enum EntitySelectorCollector implements StringIdentifiable {
  /**
   * 驯养了该实体的实体，即实体的主人，类似于 {@code /execute on owner}。
   */
  OWNER("owner", source -> source.getEntityOrThrow() instanceof Tameable tameable ? stream(tameable.getOwner()) : Stream.of()),
  /**
   * 该实体骑乘的实体，类似于 {@code /execute on vehicle}。
   */
  VEHICLE("vehicle", source -> stream(source.getEntityOrThrow().getVehicle())),
  /**
   * 骑乘该实体的所有实体，类似于 {@code /execute on passengers}。
   */
  PASSENGERS("passengers", source -> source.getEntityOrThrow().getPassengerList().stream()),
  /**
   * 使用拴绳拴住了此实体的实体，类似于 {@code /execute on leasher}。
   */
  LEASHER("leasher", source -> source.getEntityOrThrow() instanceof MobEntity mobEntity ? stream(mobEntity.getHoldingEntity()) : Stream.of()),
  /**
   * 实体的来源，例如抛出了该珍珠的玩家，类似于 {@code /execute on origin}。
   */
  ORIGIN("origin", source -> source.getEntityOrThrow() instanceof Ownable ownable ? stream(ownable.getOwner()) : Stream.of()),
  /**
   * 实体的攻击者，类似于 {@code /execute on attacker}。
   */
  ATTACKER("attacker", source -> source.getEntityOrThrow() instanceof Attackable attackable ? stream(attackable.getLastAttacker()) : Stream.of()),
  /**
   * 实体的攻击目标，类似于 {@code /execute on target}。
   */
  TARGET("target", source -> source.getEntityOrThrow() instanceof Targeter targeter ? stream(targeter.getTarget()) : Stream.of()),
  /**
   * 该实体骑乘并控制着的实体。
   */
  CONTROLLING_VEHICLE("controlling_vehicle", source -> stream(source.getEntityOrThrow().getControllingVehicle())),
  /**
   * 实体并控制着该实体的乘客，类似于 {@code /execute on controller}。
   */
  CONTROLLER("controller", source -> stream(source.getEntityOrThrow().getControllingPassenger()));

  /**
   * 该名称应当于对应的实体选择器的名称一致。
   */
  private final String name;
  private final FailableFunction<ServerCommandSource, Stream<? extends Entity>, CommandSyntaxException> entityCollector;
  private final FailableFunction<ServerCommandSource, Stream<ServerPlayerEntity>, CommandSyntaxException> playerCollector;
  public static final ImmutableMap<String, EntitySelectorCollector> NAMES = Arrays.stream(values()).collect(ImmutableMap.toImmutableMap(EntitySelectorCollector::asString, Function.identity()));

  EntitySelectorCollector(String name, FailableFunction<ServerCommandSource, Stream<? extends Entity>, CommandSyntaxException> entityCollector, FailableFunction<ServerCommandSource, Stream<ServerPlayerEntity>, CommandSyntaxException> playerCollector) {
    this.name = name;
    this.entityCollector = entityCollector;
    this.playerCollector = playerCollector;
  }

  EntitySelectorCollector(String name, FailableFunction<ServerCommandSource, Stream<? extends Entity>, CommandSyntaxException> entityCollector) {
    this(name, entityCollector, source -> entityCollector.apply(source).filter(entity -> entity instanceof ServerPlayerEntity).map(entity -> (ServerPlayerEntity) entity));
  }

  public Stream<? extends Entity> collectEntities(ServerCommandSource source) throws CommandSyntaxException {
    return entityCollector.apply(source);
  }

  public Stream<ServerPlayerEntity> collectPlayers(ServerCommandSource source) throws CommandSyntaxException {
    return playerCollector.apply(source);
  }

  @Override
  public String asString() {
    return name;
  }

  /**
   * 将 {@code null} 元素转化为空列表，将非 {@code null} 元素转化为单元素的列表。
   */
  private static <T> Stream<@NotNull T> stream(@Nullable T element) {
    return element == null ? Stream.empty() : Stream.of(element);
  }
}
