package pers.solid.ecmd.entity.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.Unmodifiable;
import pers.solid.ecmd.mixins.accessor.EntitySelectorAccessor;
import pers.solid.ecmd.mixins.accessor.EntitySelectorParserAccessor;
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
   * 类似于 {@link EntitySelectorParser#parse()}，但是允许省略开头的“@e”等变量。
   */
  public static EntitySelector readOmittibleEntitySelector(EntitySelectorParser entitySelectorReader) throws CommandSyntaxException {
    final var accessor = (EntitySelectorParserAccessor) entitySelectorReader;
    final StringReader stringReader = entitySelectorReader.getReader();

    entitySelectorReader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> {
      suggestionsBuilderConsumer.accept(suggestionsBuilder);
      suggestionsBuilder.suggest("[");
      return suggestionsBuilder.buildFuture();
    });
    if (stringReader.canRead() && stringReader.peek() == '[') {
      stringReader.skip();
      entitySelectorReader.setIncludesEntities(true);
      entitySelectorReader.setMaxResults(Integer.MAX_VALUE);
      accessor.setUsesSelectors(true);
      accessor.callParseOptions();
      ((EntitySelectorParserAccessor) entitySelectorReader).callFinalizePredicates();
      return entitySelectorReader.getSelector();
    } else {
      return entitySelectorReader.parse();
    }
  }

  /**
   * <p>将实体选择器对象中未存储于 {@link EntitySelector#contextFreePredicates} 中的一些属性转换为相应的 {@link SpecialEntityPredicate}，从而实现序列化。
   *
   * @see EntitySelectorExtras#getSpecialEntries()
   */
  public static @Unmodifiable List<SpecialEntityPredicate> calculateSpecialEntries(EntitySelector entitySelector) {
    final ImmutableList.Builder<SpecialEntityPredicate> entries = new ImmutableList.Builder<>();
    final var accessor = (EntitySelectorAccessor) entitySelector;

    if (entitySelector.extension$ec().collector != null) {
      entries.add(new CollectorEntityPredicate(entitySelector.extension$ec().collector));
    }
    if (!accessor.getRange().isAny()) {
      entries.add(new DistanceEntityPredicate(accessor.getRange(), entitySelector.extension$ec().positionOffsetInfo));
    }
    if (accessor.getAabb() != null) {
      entries.add(new BoxEntityPredicate(accessor.getAabb(), entitySelector.extension$ec().positionOffsetInfo));
    }

    if (entitySelector.isSelfSelector()) {
      entries.add(SenderOnlyEntityPredicate.INSTANCE);
    }
    if (accessor.getPlayerName() != null) {
      entries.add(new PlayerNameEntityPredicate(accessor.getPlayerName()));
    }
    if (accessor.getEntityUUID() != null) {
      entries.add(new UuidEntityPredicateEntry(accessor.getEntityUUID()));
    }

    return entries.build();
  }

  /**
   * <p>将 {@link EntitySelector#contextFreePredicates} 列表转换为可被本模组直接序列化的 {@link EntityPredicate} 对象。当列表中的 {@code Predicate<Entity>} 符合以下条件之一时，会被本模组读取：
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
    return ((EntitySelectorAccessor) entitySelector).getContextFreePredicates()
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
    final boolean includesNonPlayers = entitySelector.includesEntities();

    final EntitySelectorExtras extras = entitySelector.extension$ec();
    final List<EntityPredicate> standardPredicates = extras.getStandardPredicates();

    if (accessor.getPlayerName() != null && standardPredicates.isEmpty()) {
      return accessor.getPlayerName();
    } else if (accessor.getEntityUUID() != null && standardPredicates.isEmpty()) {
      return accessor.getEntityUUID().toString();
    }

    final StringJoiner joiner = new StringJoiner(", ", "[", "]").setEmptyValue("");
    boolean requireAlive = false;
    final EntitySelectorCollector collector = extras.collector;
    final int limit = entitySelector.getMaxResults();
    boolean hasExplicitLimit = false;
    boolean hasExplicitSorter = false;
    String atVariable = null;
    final BiConsumer<Vec3, List<? extends Entity>> sorter = accessor.getOrder();

    if (extras.collectorOf != null) {
      joiner.add("of=" + express(extras.collectorOf));
    }

    if (limit < Integer.MAX_VALUE && !(collector != null && EntitySelectorTypeExtras.FORCE_ONE_LIMIT.contains(collector.getSerializedName())) && !entitySelector.isSelfSelector()) {
      if (!EntitySelectorParser.ORDER_NEAREST.equals(sorter) && !(!includesNonPlayers && EntitySelectorParser.ORDER_RANDOM.equals(sorter))) {
        joiner.add("limit=" + limit);
        hasExplicitLimit = true;
      }
    }
    if (!EntitySelector.ORDER_ARBITRARY.equals(sorter)) {
      if (EntitySelectorParser.ORDER_NEAREST.equals(sorter)) {
        atVariable = includesNonPlayers ? "n" : "p";
      } else if (EntitySelectorParser.ORDER_RANDOM.equals(sorter) && !includesNonPlayers) {
        atVariable = "r";
      } else {
        joiner.add("sort=" + CodecUtil.SORTER_MAP.inverse().get(sorter));
        hasExplicitSorter = true;
      }
    }

    final PositionOffsetInfo positionOffsetInfo = extras.positionOffsetInfo;
    if (positionOffsetInfo != PositionOffsetInfo.NO_OP) {
      if (positionOffsetInfo.x() != null) {
        joiner.add("x=" + StringUtil.nf.format(positionOffsetInfo.x()));
      }
      if (positionOffsetInfo.y() != null) {
        joiner.add("y=" + StringUtil.nf.format(positionOffsetInfo.y()));
      }
      if (positionOffsetInfo.z() != null) {
        joiner.add("z=" + StringUtil.nf.format(positionOffsetInfo.z()));
      }
    }

    final Vec3 dxDyDz = extras.dxDyDz;
    if (dxDyDz != null) {
      if (dxDyDz.x != 0) {
        joiner.add("dx=" + StringUtil.nf.format(dxDyDz.x));
      }
      if (dxDyDz.y != 0) {
        joiner.add("dy=" + StringUtil.nf.format(dxDyDz.y));
      }
      if (dxDyDz.z != 0) {
        joiner.add("dz=" + StringUtil.nf.format(dxDyDz.z));
      }
    }

    final MinMaxBounds.Doubles distance = accessor.getRange();
    if (!distance.isAny()) {
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
      atVariable = collector.getSerializedName();
    } else if (atVariable == null) {
      if (entitySelector.isSelfSelector()) {
        atVariable = "s";
      } else if (!includesNonPlayers && accessor.getType() == EntityType.PLAYER && !requireAlive && !hasExplicitType) {
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

    if (o1.getMaxResults() != o2.getMaxResults()
        || o1.includesEntities() != o2.includesEntities()
        || o1.isWorldLimited() != o2.isWorldLimited()
        || !Objects.equals(e1.getStandardPredicates(), e2.getStandardPredicates())
        || !Objects.equals(a1.getRange(), a2.getRange())
        || !Objects.equals(e1.positionOffsetInfo, e2.positionOffsetInfo)
        || !Objects.equals(e1.dxDyDz, e2.dxDyDz)
        || a1.getOrder() != a2.getOrder()
        || o1.isSelfSelector() != o2.isSelfSelector()
        || !Objects.equals(a1.getPlayerName(), a2.getPlayerName())
        || !Objects.equals(a1.getEntityUUID(), a2.getEntityUUID())
        || !Objects.equals(a1.getType(), a2.getType())
        || o1.usesSelector() != o2.usesSelector()) {
      return false;
    }

    if (!Objects.equals(e1.collector, e2.collector)) {
      return false;
    }

    if (!Objects.equals(a1.getAabb(), a2.getAabb())) {
      EntitySelectorExtras.LOGGER.warn("Two entity selectors have the same distance, xyz and dxDyDz, but the boxes are different: distance1={}, distance2={}, xyz1={}, xyz2={}, box1={}, box2={}", a1.getRange(), a2.getRange(), e1.positionOffsetInfo, e2.positionOffsetInfo, e1.dxDyDz, e2.dxDyDz);
      return false;
    }

    return true;
  }

  public static int hashCode(EntitySelector o) {
    final EntitySelectorAccessor a = (EntitySelectorAccessor) o;
    final EntitySelectorExtras e = o.extension$ec();

    final HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
        .append(o.getMaxResults())
        .append(o.includesEntities())
        .append(o.isWorldLimited())
        .append(e.getStandardPredicates())
        .append(a.getRange())
        .append(e.positionOffsetInfo)
        .append(e.dxDyDz)
        .append(a.getOrder())
        .append(o.isSelfSelector())
        .append(a.getPlayerName())
        .append(a.getEntityUUID())
        .append(a.getType());

    hashCodeBuilder.append(e.collector);

    return hashCodeBuilder.toHashCode();
  }
}
