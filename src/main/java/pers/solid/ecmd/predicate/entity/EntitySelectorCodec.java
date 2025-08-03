package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.serialization.*;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.EntitySelectorAccessor;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EntitySelectorCodec extends MapCodec<EntitySelector> {
  public static final EntitySelectorCodec INSTANCE = new EntitySelectorCodec();
  private static final MapCodec<Integer> LIMIT = Codec.INT.optionalFieldOf("limit", Integer.MAX_VALUE);
  private static final MapCodec<Boolean> USES_AT = Codec.BOOL.optionalFieldOf("uses_at", false);
  /**
   * 处理实体谓词项的列表的 codec，该列表既包括常规的实体谓词（存储于 {@link EntitySelector#predicates}），也包括特殊的谓词（由实体选择器的参数决定）。
   */
  private static final MapCodec<List<EntityPredicate>> PREDICATES = EntityPredicate.CODEC.listOf().fieldOf("predicates");
  public static final MapCodec<NumberRange.DoubleRange> DISTANCE = NumberRange.DoubleRange.CODEC.optionalFieldOf("distance", NumberRange.DoubleRange.ANY);
  public static final MapCodec<PositionOffsetInfo> POSITION_OFFSET = PositionOffsetInfo.CODEC;

  public static final MapCodec<Optional<Double>> DX = Codec.DOUBLE.optionalFieldOf("dx");
  public static final MapCodec<Optional<Double>> DY = Codec.DOUBLE.optionalFieldOf("dy");
  public static final MapCodec<Optional<Double>> DZ = Codec.DOUBLE.optionalFieldOf("dz");

  /**
   * 用于序列化实体选择器中的 {@link EntitySelector#sorter}，即实体选择器中的 {@code sort} 参数。
   */
  public static final Codec<BiConsumer<Vec3d, List<? extends Entity>>> SORTER_CODEC = Codec.STRING.flatXmap(string -> switch (string) {
    case "arbitrary" -> DataResult.success(EntitySelector.ARBITRARY);
    case "random" -> DataResult.success(EntitySelectorReader.RANDOM);
    case "nearest" -> DataResult.success(EntitySelectorReader.NEAREST);
    case "furthest" -> DataResult.success(EntitySelectorReader.FURTHEST);
    default -> DataResult.error(() -> "unknown sorter: " + string + ", which may be provided by other mods and cannot be recognized by Enhanced Commands mod");
  }, biConsumer -> {
    if (biConsumer == EntitySelector.ARBITRARY) {
      return DataResult.success("arbitrary");
    } else if (biConsumer == EntitySelectorReader.RANDOM) {
      return DataResult.success("random");
    } else if (biConsumer == EntitySelectorReader.NEAREST) {
      return DataResult.success("nearest");
    } else if (biConsumer == EntitySelectorReader.FURTHEST) {
      return DataResult.success("furthest");
    } else {
      return DataResult.error(() -> "Unknown sorter which may be provided or modified by other mods and cannot be recognized by Enhanced Commands mod");
    }
  });

  public static final MapCodec<BiConsumer<Vec3d, List<? extends Entity>>> SORT = SORTER_CODEC.optionalFieldOf("sort", EntitySelector.ARBITRARY);

  public static final MapCodec<Optional<EntitySelectorCollector>> COLLECTOR = EntitySelectorCollector.CODEC.optionalFieldOf("collector");

  @Override
  public <T> Stream<T> keys(DynamicOps<T> ops) {
    return Stream.empty();
  }

  @Override
  public <T> DataResult<EntitySelector> decode(DynamicOps<T> ops, MapLike<T> input) {
    final DataResult<Integer> limit = LIMIT.decode(ops, input);
    if (limit instanceof DataResult.Error<Integer> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    final DataResult<Boolean> usesAt = USES_AT.decode(ops, input);
    if (usesAt instanceof DataResult.Error<Boolean> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    final DataResult<PositionOffsetInfo> positionOffset = POSITION_OFFSET.decode(ops, input);
    if (positionOffset instanceof DataResult.Error<PositionOffsetInfo> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    final DataResult<List<EntityPredicate>> predicates = PREDICATES.decode(ops, input);
    if (predicates instanceof DataResult.Error<List<EntityPredicate>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    final DataResult<NumberRange.DoubleRange> distance = DISTANCE.decode(ops, input);
    if (distance instanceof DataResult.Error<NumberRange.DoubleRange> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    boolean senderOnly = false;
    @Nullable String playerName = null;
    @Nullable UUID uuid = null;
    @Nullable EntityType<?> type = null;
    boolean localWorldOnly = false;
    boolean playerOnly = false;

    final ImmutableList.Builder<Predicate<Entity>> vanillaEntries = new ImmutableList.Builder<>();
    final MutableObject<ExecutionContext> contextWrapper = new MutableObject<>();
    for (EntityPredicate entityPredicate : predicates.getOrThrow()) {
      if (entityPredicate instanceof SenderOnlyEntityPredicate) {
        senderOnly = true;
      } else if (entityPredicate instanceof PlayerNameEntityPredicate playerNameEntityPredicate) {
        playerName = playerNameEntityPredicate.name();
      } else if (entityPredicate instanceof TypeEntityPredicateEntry typeEntityPredicateEntry && !typeEntityPredicateEntry.inverted()) {
        type = typeEntityPredicateEntry.entityType();
      } else if (entityPredicate instanceof UuidEntityPredicateEntry uuidEntityPredicateEntry) {
        uuid = uuidEntityPredicateEntry.uuid();
      } else if (entityPredicate instanceof PlayerOnlyEntityPredicate) {
        playerOnly = true;
      } else if (entityPredicate instanceof LocalWorldOnlyEntityPredicate) {
        localWorldOnly = true;
      } else if (entityPredicate instanceof SpecialEntityPredicate) {
        EntitySelectorExtras.LOGGER.warn("Found SpecialEntityPredicate that is not identified in Enhanced Commands Mod serialization: {}", entityPredicate);
      } else if (entityPredicate instanceof StaticEntityPredicate direct) {
        vanillaEntries.add(direct);
      } else if (entityPredicate instanceof EntityPredicateEntry entry) {
        vanillaEntries.add(new DynamicEntityPredicateWrapper(entry, contextWrapper));
      }
    }

    final DataResult<Optional<Double>> dx = DX.decode(ops, input);
    if (dx instanceof DataResult.Error<Optional<Double>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());
    final DataResult<Optional<Double>> dy = DY.decode(ops, input);
    if (dy instanceof DataResult.Error<Optional<Double>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());
    final DataResult<Optional<Double>> dz = DZ.decode(ops, input);
    if (dz instanceof DataResult.Error<Optional<Double>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    Box box;
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
    }

    final DataResult<BiConsumer<Vec3d, List<? extends Entity>>> sort = SORT.decode(ops, input);
    if (sort instanceof DataResult.Error<BiConsumer<Vec3d, List<? extends Entity>>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    final DataResult<Optional<EntitySelectorCollector>> collector = COLLECTOR.decode(ops, input);
    if (collector instanceof DataResult.Error<Optional<EntitySelectorCollector>> error) return DataResult.error(error.messageSupplier(), error.lifecycle());

    final EntitySelector entitySelector = new EntitySelector(limit.getOrThrow(), !playerOnly, localWorldOnly, vanillaEntries.build(), distance.getOrThrow(), positionOffset.getOrThrow(), box, sort.getOrThrow(), senderOnly, playerName, uuid, type, usesAt.getOrThrow());
    entitySelector.extension$ec().positionOffsetInfo = positionOffset.getOrThrow();
    entitySelector.extension$ec().contextWrapper = contextWrapper;
    entitySelector.extension$ec().collector = collector.getOrThrow().orElse(null);
    return DataResult.success(entitySelector);
  }

  @Override
  public <T> RecordBuilder<T> encode(EntitySelector input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
    LIMIT.encode(input.getLimit(), ops, prefix);
    USES_AT.encode(input.usesAt(), ops, prefix);
    POSITION_OFFSET.encode(input.extension$ec().positionOffsetInfo, ops, prefix);

    final NumberRange.DoubleRange distance = ((EntitySelectorAccessor) input).getDistance();

    final Vec3d dxDyDz = input.extension$ec().dxDyDz;
    if (dxDyDz != null) {
      DX.encode(Optional.of(dxDyDz.x), ops, prefix);
      DY.encode(Optional.of(dxDyDz.y), ops, prefix);
      DZ.encode(Optional.of(dxDyDz.z), ops, prefix);
    }

    final List<SpecialEntityPredicate> specialEntries = EntitySelectorHelper.getSpecialEntries(input);
    final List<EntityPredicate> serializablePredicates = EntitySelectorHelper.getStandardPredicates(input);
    PREDICATES.encode(ImmutableList.copyOf(Iterables.concat(specialEntries, serializablePredicates)), ops, prefix);
    DISTANCE.encode(distance, ops, prefix);

    SORT.encode(((EntitySelectorAccessor) input).getSorter(), ops, prefix);
    COLLECTOR.encode(Optional.ofNullable(input.extension$ec().collector), ops, prefix);

    return prefix;
  }
}
