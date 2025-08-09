package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.*;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.Registries;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.EntitySelectorAccessor;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EntitySelectorCodec extends MapCodec<EntitySelector> {
  public static final EntitySelectorCodec INSTANCE = new EntitySelectorCodec();

  // 下面这些字段的顺序尽量保持与 EntitySelector 的构造函数一致。

  // region standard fields
  private static final MapCodec<Integer> LIMIT = Codec.INT.optionalFieldOf("limit", Integer.MAX_VALUE);
  private static final MapCodec<Boolean> PLAYER_ONLY = Codec.BOOL.optionalFieldOf("player_only", false);
  private static final MapCodec<Boolean> LOCAL_WORLD_ONLY = Codec.BOOL.optionalFieldOf("local_world_only", false);
  private static final MapCodec<List<EntityPredicate>> PREDICATES = EntityPredicate.CODEC.listOf().optionalFieldOf("predicates", List.of());
  private static final MapCodec<NumberRange.DoubleRange> DISTANCE = NumberRange.DoubleRange.CODEC.optionalFieldOf("distance", NumberRange.DoubleRange.ANY);
  private static final MapCodec<PositionOffsetInfo> POSITION_OFFSET = PositionOffsetInfo.CODEC;
  private static final MapCodec<Optional<Double>> DX = Codec.DOUBLE.optionalFieldOf("dx");
  private static final MapCodec<Optional<Double>> DY = Codec.DOUBLE.optionalFieldOf("dy");
  private static final MapCodec<Optional<Double>> DZ = Codec.DOUBLE.optionalFieldOf("dz");
  private static final MapCodec<BiConsumer<Vec3d, List<? extends Entity>>> SORT = CodecUtil.SORTER.optionalFieldOf("sort", EntitySelector.ARBITRARY);
  private static final MapCodec<Boolean> SENDER_ONLY = Codec.BOOL.optionalFieldOf("sender_only", false);
  private static final MapCodec<Optional<String>> PLAYER_NAME = Codec.STRING.optionalFieldOf("player_name");
  private static final MapCodec<Optional<UUID>> UUID = Uuids.CODEC.optionalFieldOf("uuid");
  private static final MapCodec<Optional<EntityType<?>>> ENTITY_TYPE = Registries.ENTITY_TYPE.getCodec().optionalFieldOf("entity_type");
  private static final MapCodec<Boolean> USES_AT = Codec.BOOL.optionalFieldOf("uses_at", false);
  // endregion standard fields

  public static final MapCodec<Optional<EntitySelectorCollector>> COLLECTOR = EntitySelectorCollector.CODEC.optionalFieldOf("collector");
  public static final MapCodec<Optional<EntitySelector>> COLLECTOR_OF = EntitySelectorCodec.INSTANCE.codec().optionalFieldOf("collector_of");

  @Override
  public <T> Stream<T> keys(DynamicOps<T> ops) {
    return Stream.empty();
  }

  @Override
  public <T> DataResult<EntitySelector> decode(DynamicOps<T> ops, MapLike<T> input) {
    // limit
    final DataResult<Integer> limit = LIMIT.decode(ops, input);
    if (limit instanceof DataResult.Error<Integer> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // playerOnly
    final DataResult<Boolean> playerOnly = PLAYER_ONLY.decode(ops, input);
    if (playerOnly instanceof DataResult.Error<Boolean> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // localWorldOnly
    final DataResult<Boolean> localWorldOnly = LOCAL_WORLD_ONLY.decode(ops, input);
    if (localWorldOnly instanceof DataResult.Error<Boolean> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // predicates
    final DataResult<List<EntityPredicate>> predicates = PREDICATES.decode(ops, input);
    if (predicates instanceof DataResult.Error<List<EntityPredicate>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());
    final ImmutableList.Builder<Predicate<Entity>> vanillaEntries = new ImmutableList.Builder<>();
    final MutableObject<ExecutionContext> contextWrapper = new MutableObject<>();
    for (EntityPredicate entityPredicate : predicates.getOrThrow()) {
      if (entityPredicate instanceof SpecialEntityPredicate) {
        EntitySelectorExtras.LOGGER.warn("Found SpecialEntityPredicate that should not be serialized in the entity predicate: {}", entityPredicate);
      } else if (entityPredicate instanceof StaticEntityPredicate direct) {
        vanillaEntries.add(direct);
      } else if (entityPredicate instanceof EntityPredicateEntry entry) {
        vanillaEntries.add(new DynamicEntityPredicateWrapper(entry, contextWrapper));
      }
    }

    // distance
    final DataResult<NumberRange.DoubleRange> distance = DISTANCE.decode(ops, input);
    if (distance instanceof DataResult.Error<NumberRange.DoubleRange> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // positionOffset
    final DataResult<PositionOffsetInfo> positionOffset = POSITION_OFFSET.decode(ops, input);
    if (positionOffset instanceof DataResult.Error<PositionOffsetInfo> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // dx dy dz
    final DataResult<Optional<Double>> dx = DX.decode(ops, input);
    if (dx instanceof DataResult.Error<Optional<Double>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());
    final DataResult<Optional<Double>> dy = DY.decode(ops, input);
    if (dy instanceof DataResult.Error<Optional<Double>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());
    final DataResult<Optional<Double>> dz = DZ.decode(ops, input);
    if (dz instanceof DataResult.Error<Optional<Double>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // region calculate box
    Box box;
    @Nullable Vec3d dxDyDz = null;
    if (dx.getOrThrow().isEmpty() && dz.getOrThrow().isEmpty() && dz.getOrThrow().isEmpty()) {
      if (distance.getOrThrow() != null && distance.getOrThrow().max().isPresent()) {
        double maxDistance = distance.getOrThrow().max().get();
        box = new Box(-maxDistance, -maxDistance, -maxDistance, maxDistance + 1d, maxDistance + 1d, maxDistance + 1d);
      } else {
        box = null;
      }
    } else {
      double x = dx.getOrThrow().orElse(0d);
      double y = dy.getOrThrow().orElse(0d);
      double z = dz.getOrThrow().orElse(0d);
      boolean negativeX = x < 0d;
      boolean negativeY = y < 0d;
      boolean negativeZ = z < 0d;
      double x1 = negativeX ? x : 0d;
      double y1 = negativeY ? y : 0d;
      double z1 = negativeZ ? z : 0d;
      double x2 = (negativeX ? 0d : x) + 1d;
      double y2 = (negativeY ? 0d : y) + 1d;
      double z2 = (negativeZ ? 0d : z) + 1d;
      box = new Box(x1, y1, z1, x2, y2, z2);
      dxDyDz = new Vec3d(dx.getOrThrow().orElse(0d), dy.getOrThrow().orElse(0d), dz.getOrThrow().orElse(0d));
    }
    // endregion calculate box

    // sort
    final DataResult<BiConsumer<Vec3d, List<? extends Entity>>> sort = SORT.decode(ops, input);
    if (sort instanceof DataResult.Error<BiConsumer<Vec3d, List<? extends Entity>>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // senderOnly
    final DataResult<Boolean> senderOnly = SENDER_ONLY.decode(ops, input);
    if (senderOnly instanceof DataResult.Error<Boolean> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // playerName
    final DataResult<Optional<String>> playerName = PLAYER_NAME.decode(ops, input);
    if (playerName instanceof DataResult.Error<Optional<String>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // uuid
    final DataResult<Optional<UUID>> uuid = UUID.decode(ops, input);
    if (uuid instanceof DataResult.Error<Optional<UUID>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // entityType
    final DataResult<Optional<EntityType<?>>> entityType = ENTITY_TYPE.decode(ops, input);
    if (entityType instanceof DataResult.Error<Optional<EntityType<?>>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // useAt
    final DataResult<Boolean> usesAt = USES_AT.decode(ops, input);
    if (usesAt instanceof DataResult.Error<Boolean> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // collector
    final DataResult<Optional<EntitySelectorCollector>> collector = COLLECTOR.decode(ops, input);
    if (collector instanceof DataResult.Error<Optional<EntitySelectorCollector>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    // collector of
    final DataResult<Optional<EntitySelector>> collectorOf = COLLECTOR_OF.decode(ops, input);
    if (collectorOf instanceof DataResult.Error<Optional<EntitySelector>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    final EntitySelector entitySelector = new EntitySelector(limit.getOrThrow(), !playerOnly.getOrThrow(), localWorldOnly.getOrThrow(), vanillaEntries.build(), distance.getOrThrow(), positionOffset.getOrThrow(), box, sort.getOrThrow(), senderOnly.getOrThrow(), playerName.getOrThrow().orElse(null), uuid.getOrThrow().orElse(null), entityType.getOrThrow().orElse(null), usesAt.getOrThrow());
    entitySelector.extension$ec().positionOffsetInfo = positionOffset.getOrThrow();
    entitySelector.extension$ec().dxDyDz = dxDyDz;
    entitySelector.extension$ec().contextWrapper = contextWrapper;
    entitySelector.extension$ec().collector = collector.getOrThrow().orElse(null);
    entitySelector.extension$ec().collectorOf = collectorOf.getOrThrow().orElse(null);
    return DataResult.success(entitySelector);
  }

  @Override
  public <T> RecordBuilder<T> encode(EntitySelector input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
    LIMIT.encode(input.getLimit(), ops, prefix);
    PLAYER_ONLY.encode(!input.includesNonPlayers(), ops, prefix);
    LOCAL_WORLD_ONLY.encode(input.isLocalWorldOnly(), ops, prefix);
    PREDICATES.encode(input.extension$ec().getStandardPredicates(), ops, prefix);
    DISTANCE.encode(((EntitySelectorAccessor) input).getDistance(), ops, prefix);
    POSITION_OFFSET.encode(input.extension$ec().positionOffsetInfo, ops, prefix);

    final Vec3d dxDyDz = input.extension$ec().dxDyDz;
    if (dxDyDz != null) {
      DX.encode(Optional.of(dxDyDz.x), ops, prefix);
      DY.encode(Optional.of(dxDyDz.y), ops, prefix);
      DZ.encode(Optional.of(dxDyDz.z), ops, prefix);
    }
    SORT.encode(((EntitySelectorAccessor) input).getSorter(), ops, prefix);
    SENDER_ONLY.encode(input.isSenderOnly(), ops, prefix);
    PLAYER_NAME.encode(Optional.ofNullable(((EntitySelectorAccessor) input).getPlayerName()), ops, prefix);
    UUID.encode(Optional.ofNullable(((EntitySelectorAccessor) input).getUuid()), ops, prefix);
    ENTITY_TYPE.encode(Optional.ofNullable(((EntitySelectorAccessor) input).getEntityFilter() instanceof EntityType<?> entityType ? entityType : null), ops, prefix);
    USES_AT.encode(input.usesAt(), ops, prefix);

    COLLECTOR.encode(Optional.ofNullable(input.extension$ec().collector), ops, prefix);
    COLLECTOR_OF.encode(Optional.ofNullable(input.extension$ec().collectorOf), ops, prefix);

    return prefix;
  }
}
