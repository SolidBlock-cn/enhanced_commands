package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.mixins.accessor.EntitySelectorAccessor;
import pers.solid.ecmd.util.StringUtil;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

/**
 * 本类包含与实体选择器的序列化、信息获取等有关的辅助方法。
 */
public final class EntitySelectorHelper {
  private EntitySelectorHelper() {
  }

  /**
   * <p>将实体选择器对象中未存储于 {@link EntitySelector#predicates} 中的一些属性转换为相应的 {@link SpecialEntityPredicate}，从而实现序列化。
   *
   * @see EntitySelectorExtras#getSpecialEntries()
   */
  public static @Unmodifiable List<SpecialEntityPredicate> calculateSpecialEntries(EntitySelector entitySelector) {
    final ImmutableList.Builder<SpecialEntityPredicate> entries = new ImmutableList.Builder<>();
    final var accessor = (EntitySelectorAccessor) entitySelector;

    if (!accessor.getDistance().isDummy()) {
      entries.add(new DistanceBlockPredicate(accessor.getDistance(), entitySelector.extension$ec().positionOffsetInfo));
    }
    if (accessor.getBox() != null) {
      entries.add(new BoxEntityPredicate(accessor.getBox(), entitySelector.extension$ec().positionOffsetInfo));
    }

    if (!entitySelector.includesNonPlayers()) {
      entries.add(PlayerOnlyEntityPredicate.INSTANCE);
    }
    if (entitySelector.isLocalWorldOnly()) {
      entries.add(LocalWorldOnlyEntityPredicate.INSTANCE);
    }
    if (entitySelector.isSenderOnly()) {
      entries.add(SenderOnlyEntityPredicate.INSTANCE);
    }
    if (accessor.getPlayerName() != null) {
      entries.add(new PlayerNameEntityPredicate(accessor.getPlayerName()));
    }
    if (accessor.getUuid() != null) {
      entries.add(new UuidEntityPredicateEntry(accessor.getUuid()));
    }
    if (accessor.getEntityFilter() instanceof EntityType<?> entityType) {
      entries.add(new TypeEntityPredicateEntry(entityType, false));
    }

    return entries.build();
  }

  /**
   * <p>将 {@link EntitySelector#predicates} 列表转换为可被本模组直接序列化的 {@link EntityPredicate} 对象。当列表中的 {@code Predicate<Entity>} 符合以下条件之一时，会被本模组读取：
   * <ul>
   *   <li>直接继承了 {@link EntityPredicate}。</li>
   *   <li>继承了 {@link StaticEntityPredicateWrapper}。</li>
   *   <li>继承了 {@link DynamicEntityPredicateWrapper}。</li>
   * </ul>
   *
   * <p>如果上述条件均不符合，会被本模组转换成 {@link UnknownEntityPredicateEntry}。
   *
   * @see EntitySelectorExtras#getStandardPredicates()
   */
  public static @Unmodifiable List<EntityPredicate> calculateStandardPredicates(EntitySelector entitySelector) {
    return ((EntitySelectorAccessor) entitySelector).getPredicates()
        .stream()
        .map(predicate -> {
          if (predicate instanceof EntityPredicate entry) {
            return entry;
          } else if (predicate instanceof StaticEntityPredicateWrapper wrapper) {
            return wrapper.entityPredicate();
          } else if (predicate instanceof DynamicEntityPredicateWrapper wrapper) {
            return wrapper.entityPredicate();
          } else {
            return new UnknownEntityPredicateEntry(predicate);
          }
        })
        .collect(ImmutableList.toImmutableList());
  }

  public static String express(EntitySelector entitySelector) {
    final EntitySelectorAccessor accessor = (EntitySelectorAccessor) entitySelector;
    final boolean includesNonPlayers = entitySelector.includesNonPlayers();

    final List<EntityPredicate> standardPredicates = entitySelector.extension$ec().getStandardPredicates();

    if (accessor.getPlayerName() != null && standardPredicates.isEmpty()) {
      return accessor.getPlayerName();
    } else if (accessor.getUuid() != null && standardPredicates.isEmpty()) {
      return accessor.getUuid().toString();
    }

    final StringJoiner joiner = new StringJoiner(", ", "[", "]").setEmptyValue("");
    boolean requireAlive = false;
    final EntitySelectorCollector collector = entitySelector.extension$ec().collector;
    final int limit = entitySelector.getLimit();

    if (limit < Integer.MAX_VALUE && !(collector != null && EntitySelectorTypeExtras.FORCE_ONE_LIMIT.contains(collector.asString())) && !entitySelector.isSenderOnly()) {
      joiner.add("limit=" + limit);
    }
    final BiConsumer<Vec3d, List<? extends Entity>> sorter = accessor.getSorter();
    if (sorter == EntitySelectorReader.RANDOM) {
      joiner.add("sort=random");
    } else if (sorter == EntitySelectorReader.NEAREST) {
      joiner.add("sort=nearest");
    } else if (sorter == EntitySelectorReader.FURTHEST) {
      joiner.add("sort=furthest");
    }

    final PositionOffsetInfo positionOffsetInfo = entitySelector.extension$ec().positionOffsetInfo;
    if (positionOffsetInfo != PositionOffsetInfo.NO_OP) {
      if (positionOffsetInfo.x() != null) {
        joiner.add("x=" + positionOffsetInfo.x());
      }
      if (positionOffsetInfo.y() != null) {
        joiner.add("y=" + positionOffsetInfo.y());
      }
      if (positionOffsetInfo.z() != null) {
        joiner.add("z=" + positionOffsetInfo.z());
      }
    }

    final Vec3d dxDyDz = entitySelector.extension$ec().dxDyDz;
    if (dxDyDz != null) {
      if (dxDyDz.x != 0) {
        joiner.add("dx=" + dxDyDz.x);
      }
      if (dxDyDz.y != 0) {
        joiner.add("dy=" + dxDyDz.y);
      }
      if (dxDyDz.z != 0) {
        joiner.add("dz=" + dxDyDz.z);
      }
    }

    final NumberRange.DoubleRange distance = accessor.getDistance();
    if (!distance.isDummy()) {
      joiner.add("distance=" + StringUtil.wrapRange(distance));
    }

    boolean hasExplicitType = false;
    for (EntityPredicate predicate : standardPredicates) {
      if (predicate instanceof AliveEntityPredicate) {
        requireAlive = true;
      } else if (predicate instanceof EntityPredicateEntry entry) {
        if (predicate instanceof TypeEntityPredicateEntry) {
          hasExplicitType = true;
        }
        joiner.add(entry.toOptionEntry());
      }
    }

    final String atVariable;
    if (collector != null) {
      atVariable = collector.asString();
    } else if (entitySelector.isSenderOnly()) {
      atVariable = "s";
    } else if (includesNonPlayers || hasExplicitType) {
      atVariable = requireAlive ? "e" : "E";
    } else {
      atVariable = "a";
    }

    return "@" + atVariable + joiner;
  }

  public static boolean equals(EntitySelector o1, EntitySelector o2) {
    final EntitySelectorAccessor a1 = (EntitySelectorAccessor) o1;
    final EntitySelectorAccessor a2 = (EntitySelectorAccessor) o2;
    final EntitySelectorExtras e1 = o1.extension$ec();
    final EntitySelectorExtras e2 = o2.extension$ec();

    if (o1.getLimit() != o2.getLimit()
        || o1.includesNonPlayers() != o2.includesNonPlayers()
        || o1.isLocalWorldOnly() != o2.isLocalWorldOnly()
        || !Objects.equals(a1.getDistance(), a2.getDistance())
        || !Objects.equals(e1.positionOffsetInfo, e2.positionOffsetInfo)
        || !Objects.equals(e1.dxDyDz, e2.dxDyDz)
        || a1.getSorter() != a2.getSorter()
        || o1.isSenderOnly() != o2.isSenderOnly()
        || !Objects.equals(a1.getPlayerName(), a2.getPlayerName())
        || !Objects.equals(a1.getUuid(), a2.getUuid())
        || !Objects.equals(a1.getEntityFilter(), a2.getEntityFilter())) {
      return false;
    }

    if (!Objects.equals(e1.collector, e2.collector)
        || !Objects.equals(e1.getStandardPredicates(), e2.getStandardPredicates())) {
      return false;
    }

    if (!Objects.equals(a1.getBox(), a2.getBox())) {
      EntitySelectorExtras.LOGGER.warn("Two entity selectors have the same distance, xyz and dxDyDz, but the boxes are different: distance1={}, distance2={}, xyz1={}, xyz2={}, box1={}, box2={}", a1.getDistance(), a2.getDistance(), e1.positionOffsetInfo, e2.positionOffsetInfo, e1.dxDyDz, e2.dxDyDz);
      return false;
    }

    return true;
  }

  public static int hashCode(EntitySelector o) {
    final EntitySelectorAccessor a = (EntitySelectorAccessor) o;
    final EntitySelectorExtras e = o.extension$ec();

    final HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
        .append(o.getLimit())
        .append(o.includesNonPlayers())
        .append(o.isLocalWorldOnly())
        .append(a.getDistance())
        .append(e.positionOffsetInfo)
        .append(e.dxDyDz)
        .append(a.getSorter())
        .append(o.isSenderOnly())
        .append(a.getPlayerName())
        .append(a.getUuid())
        .append(a.getEntityFilter());

    hashCodeBuilder.append(e.collector).append(e.getStandardPredicates());

    return hashCodeBuilder.toHashCode();
  }
}
