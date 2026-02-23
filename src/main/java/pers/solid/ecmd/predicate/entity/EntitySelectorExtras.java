package pers.solid.ecmd.predicate.entity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.mixins.mixin.EntitySelectorParserMixin;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.function.Function;

/**
 * 附加在 {@link EntitySelector} 的额外内容。
 *
 * @see pers.solid.ecmd.mixins.mixin.EntitySelectorMixin
 * @see pers.solid.ecmd.mixins.ext.EntitySelectorExtension
 */
public class EntitySelectorExtras {
  public static final Logger LOGGER = LoggerFactory.getLogger(EntitySelectorExtras.class);
  private final EntitySelector self;
  /**
   * 该实体选择器所使用的 {@link CommandSourceStack}。可能会在实际调用时发生改变。
   */
  public CommandSourceStack source;

  /**
   * 以可序列化的形式记录 {@link EntitySelector#positionOffset}，因为该字段类型为 {@link Function}，无法序列化其数据，因此需要在 {@link EntitySelectorParser#getSelector()} 中手动存储其序列化数据。
   *
   * @see EntitySelectorParserMixin#recordMoreInfoAtBuild(EntitySelector)
   */
  public @NotNull PositionOffsetInfo positionOffsetInfo = PositionOffsetInfo.NO_OP;

  /**
   * 以可序列化的形式记录 {@link EntitySelectorParser#dx}、{@link EntitySelectorParser#dy}、{@link EntitySelectorParser#dz}，因为这些数据并不会存储在 {@link EntitySelector} 中。
   */
  public @Nullable Vec3 dxDyDz = null;

  /**
   * 此字段决定了在运行 {@link EntitySelector#findEntities(CommandSourceStack)} 和 {@link EntitySelector#findPlayers(CommandSourceStack)} 时，如何以特殊的方式收集实体。
   *
   * @see EntitySelectorParserMixin#recordMoreInfoAtBuild(EntitySelector)
   */
  public @Nullable EntitySelectorCollector collector;

  /**
   * 此字段决定了在使用 {@link #collector} 时基于哪个实体特殊收集。例如：
   * <ul>
   *   <li>{@code @passengers}：当前实体的乘客</li>
   *   <li>{@code @passengers[of=@n[type=pig]]}：最近一只猪的乘客</li>
   * </ul>
   */
  public @Nullable EntitySelector collectorOf;

  public MutableObject<ExecutionContext> contextWrapper = new MutableObject<>();

  /**
   * 该实体选择实际用于判断实体的谓词对象。
   */
  private @Unmodifiable List<SpecialEntityPredicate> specialEntries = null;
  /**
   * 实体选择器的常规谓词。
   */
  private @Unmodifiable List<EntityPredicate> standardPredicates = null;

  public EntitySelectorExtras(EntitySelector self) {
    this.self = self;
  }

  public void updateSource(@NotNull CommandSourceStack source) {
    if (!source.equals(this.source)) {
      this.source = source;
      this.contextWrapper.setValue(new ExecutionContext(source));
    }
  }

  /**
   * 获取已有 {@link EntitySelector} 中的 {@link EntitySelectorExtras} 对象。当接口没有注入或者无法编译时，可以调用此方法。
   */
  public static EntitySelectorExtras getOf(EntitySelector entitySelector) {
    return entitySelector.extension$ec();
  }

  public @NotNull @Unmodifiable List<SpecialEntityPredicate> getSpecialEntries() {
    if (specialEntries == null) {
      specialEntries = EntitySelectors.calculateSpecialEntries(self);
    }
    return specialEntries;
  }

  public @NotNull @Unmodifiable List<EntityPredicate> getStandardPredicates() {
    if (standardPredicates == null) {
      standardPredicates = EntitySelectors.calculateStandardPredicates(self);
    }
    return standardPredicates;
  }
}
