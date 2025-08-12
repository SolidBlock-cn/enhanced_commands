package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.mixins.accessor.EntitySelectorAccessor;
import pers.solid.ecmd.mixins.accessor.EntitySelectorReaderAccessor;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

/**
 * 此类包含了与实体选择器有关的一些实用方法，包括与序列化、文本转换相关的实用方法。
 */
public final class EntitySelectors {
  private EntitySelectors() {
  }

  /**
   * 类似于 {@link EntitySelectorReader#read()}，但是允许省略开头的“@e”等变量。
   */
  public static EntitySelector readOmittibleEntitySelector(@NotNull EntitySelectorReader entitySelectorReader) throws CommandSyntaxException {
    final var accessor = (EntitySelectorReaderAccessor) entitySelectorReader;
    final StringReader stringReader = entitySelectorReader.getReader();

    entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> {
      suggestionsBuilderConsumer.accept(suggestionsBuilder);
      suggestionsBuilder.suggest("[");
      return suggestionsBuilder.buildFuture();
    });
    if (stringReader.canRead() && stringReader.peek() == '[') {
      stringReader.skip();
      entitySelectorReader.setIncludesNonPlayers(true);
      entitySelectorReader.setLimit(Integer.MAX_VALUE);
      accessor.setUsesAt(true);
      accessor.callReadArguments();
      ((EntitySelectorReaderAccessor) entitySelectorReader).callBuildPredicate();
      return entitySelectorReader.build();
    } else {
      return entitySelectorReader.read();
    }
  }

  /**
   * <p>将实体选择器对象中未存储于 {@link EntitySelector#predicates} 中的一些属性转换为相应的 {@link SpecialEntityPredicate}，从而实现序列化。
   *
   * @see EntitySelectorExtras#getSpecialEntries()
   */
  public static @Unmodifiable List<SpecialEntityPredicate> calculateSpecialEntries(EntitySelector entitySelector) {
    final ImmutableList.Builder<SpecialEntityPredicate> entries = new ImmutableList.Builder<>();
    final var accessor = (EntitySelectorAccessor) entitySelector;

    if (entitySelector.extension$ec().collector != null) {
      entries.add(new CollectorEntityPredicate(entitySelector.extension$ec().collector));
    }
    if (!accessor.getDistance().isDummy()) {
      entries.add(new DistanceEntityPredicate(accessor.getDistance(), entitySelector.extension$ec().positionOffsetInfo));
    }
    if (accessor.getBox() != null) {
      entries.add(new BoxEntityPredicate(accessor.getBox(), entitySelector.extension$ec().positionOffsetInfo));
    }

    if (!entitySelector.includesNonPlayers()) {
      entries.add(PlayerOnlyEntityPredicate.INSTANCE);
    }
    if (entitySelector.isLocalWorldOnly()) {
      entries.add(LocalWorldEntityPredicate.INSTANCE);
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

    final EntitySelectorExtras extras = entitySelector.extension$ec();
    final List<EntityPredicate> standardPredicates = extras.getStandardPredicates();

    if (accessor.getPlayerName() != null && standardPredicates.isEmpty()) {
      return accessor.getPlayerName();
    } else if (accessor.getUuid() != null && standardPredicates.isEmpty()) {
      return accessor.getUuid().toString();
    }

    final StringJoiner joiner = new StringJoiner(", ", "[", "]").setEmptyValue("");
    boolean requireAlive = false;
    final EntitySelectorCollector collector = extras.collector;
    final int limit = entitySelector.getLimit();
    boolean hasExplicitLimit = false;
    boolean hasExplicitSorter = false;
    String atVariable = null;
    final BiConsumer<Vec3d, List<? extends Entity>> sorter = accessor.getSorter();

    if (extras.collectorOf != null) {
      joiner.add("of=" + express(extras.collectorOf));
    }

    if (limit < Integer.MAX_VALUE && !(collector != null && EntitySelectorTypeExtras.FORCE_ONE_LIMIT.contains(collector.asString())) && !entitySelector.isSenderOnly()) {
      if (!EntitySelectorReader.NEAREST.equals(sorter) && !(!includesNonPlayers && EntitySelectorReader.RANDOM.equals(sorter))) {
        joiner.add("limit=" + limit);
        hasExplicitLimit = true;
      }
    }
    if (!EntitySelector.ARBITRARY.equals(sorter)) {
      if (EntitySelectorReader.NEAREST.equals(sorter)) {
        atVariable = includesNonPlayers ? "n" : "p";
      } else if (EntitySelectorReader.RANDOM.equals(sorter) && !includesNonPlayers) {
        atVariable = "r";
      } else {
        joiner.add("sort=" + CodecUtil.SORTER_MAP.inverse().get(sorter));
        hasExplicitSorter = true;
      }
    }

    final PositionOffsetInfo positionOffsetInfo = extras.positionOffsetInfo;
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

    final Vec3d dxDyDz = extras.dxDyDz;
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

    if (collector != null) {
      atVariable = collector.asString();
    } else if (atVariable == null) {
      if (entitySelector.isSenderOnly()) {
        atVariable = "s";
      } else if (!includesNonPlayers && accessor.getEntityFilter() == EntityType.PLAYER && !requireAlive && !hasExplicitType) {
        atVariable = "a";
      } else {
        atVariable = requireAlive ? "e" : "E";
      }
    } else if ("n".equals(atVariable) && !requireAlive) {
      if (hasExplicitType) {
        atVariable = "p";
      } else {
        atVariable = "E";
        if (!hasExplicitSorter) {
          joiner.add("sort=nearest");
        }
        if (!hasExplicitLimit) {
          joiner.add("limit=" + limit);
        }
      }
    } else if ("p".equals(atVariable) && requireAlive) {
      atVariable = "n";
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
        || !Objects.equals(e1.getStandardPredicates(), e2.getStandardPredicates())
        || !Objects.equals(a1.getDistance(), a2.getDistance())
        || !Objects.equals(e1.positionOffsetInfo, e2.positionOffsetInfo)
        || !Objects.equals(e1.dxDyDz, e2.dxDyDz)
        || a1.getSorter() != a2.getSorter()
        || o1.isSenderOnly() != o2.isSenderOnly()
        || !Objects.equals(a1.getPlayerName(), a2.getPlayerName())
        || !Objects.equals(a1.getUuid(), a2.getUuid())
        || !Objects.equals(a1.getEntityFilter(), a2.getEntityFilter())
        || o1.usesAt() != o2.usesAt()) {
      return false;
    }

    if (!Objects.equals(e1.collector, e2.collector)) {
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
        .append(e.getStandardPredicates())
        .append(a.getDistance())
        .append(e.positionOffsetInfo)
        .append(e.dxDyDz)
        .append(a.getSorter())
        .append(o.isSenderOnly())
        .append(a.getPlayerName())
        .append(a.getUuid())
        .append(a.getEntityFilter());

    hashCodeBuilder.append(e.collector);

    return hashCodeBuilder.toHashCode();
  }
}
