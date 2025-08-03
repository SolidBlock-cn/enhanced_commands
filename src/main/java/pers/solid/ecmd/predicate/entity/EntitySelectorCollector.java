package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 特殊的实体选择器从世界中收集实体的规则。当实体选择器应用了这些特殊的规则时，将按照这些规则收集实体，而非从世界中收集所有实体。
 */
public enum EntitySelectorCollector implements StringIdentifiable {
  /**
   * 驯养了该实体的实体，即实体的主人，类似于 {@code /execute on owner}。
   */
  OWNER("owner", entity -> entity instanceof Tameable tameable ? stream(tameable.getOwner()) : Stream.of()),
  /**
   * 该实体骑乘的实体，类似于 {@code /execute on vehicle}。
   */
  VEHICLE("vehicle", entity -> stream(entity.getVehicle())),
  /**
   * 骑乘该实体的所有实体，类似于 {@code /execute on passengers}。
   */
  PASSENGERS("passengers", entity -> entity.getPassengerList().stream()),
  /**
   * 使用拴绳拴住了此实体的实体，类似于 {@code /execute on leasher}。
   */
  LEASHER("leasher", entity -> entity instanceof MobEntity mobEntity ? stream(mobEntity.getLeashHolder()) : Stream.of()),
  /**
   * 实体的来源，例如抛出了该珍珠的玩家，类似于 {@code /execute on origin}。
   */
  ORIGIN("origin", entity -> entity instanceof Ownable ownable ? stream(ownable.getOwner()) : Stream.of()),
  /**
   * 实体的攻击者，类似于 {@code /execute on attacker}。
   */
  ATTACKER("attacker", entity -> entity instanceof Attackable attackable ? stream(attackable.getLastAttacker()) : Stream.of()),
  /**
   * 实体的攻击目标，类似于 {@code /execute on target}。
   */
  TARGET("target", entity -> entity instanceof Targeter targeter ? stream(targeter.getTarget()) : Stream.of()),
  /**
   * 该实体骑乘并控制着的实体。
   */
  CONTROLLING_VEHICLE("controlling_vehicle", entity -> stream(entity.getControllingVehicle())),
  /**
   * 实体并控制着该实体的乘客，类似于 {@code /execute on controller}。
   */
  CONTROLLER("controller", entity -> stream(entity.getControllingPassenger()));

  /**
   * 该名称应当于对应的实体选择器的名称一致。
   */
  private final String name;
  private final Function<Entity, Stream<? extends Entity>> entityCollector;
  private final Function<Entity, Stream<ServerPlayerEntity>> playerCollector;
  private final Text displayName;
  public static final StringIdentifiableCodec<EntitySelectorCollector> CODEC = StringIdentifiableCodec.create(values());

  EntitySelectorCollector(String name, Function<Entity, Stream<? extends Entity>> entityCollector, Function<Entity, Stream<ServerPlayerEntity>> playerCollector) {
    this.name = name;
    this.entityCollector = entityCollector;
    this.playerCollector = playerCollector;
    this.displayName = Text.translatable("enhanced_commands.entity_selector_collector." + name);
  }

  EntitySelectorCollector(String name, Function<Entity, Stream<? extends Entity>> entityCollector) {
    this(name, entityCollector, source -> entityCollector.apply(source).filter(entity -> entity instanceof ServerPlayerEntity).map(entity -> (ServerPlayerEntity) entity));
  }

  public Stream<? extends Entity> collectEntities(Entity entity) {
    return entityCollector.apply(entity);
  }

  public Stream<ServerPlayerEntity> collectPlayers(Entity entity) {
    return playerCollector.apply(entity);
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

  public Text getDisplayName() {
    return displayName;
  }
}
