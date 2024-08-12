package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.EntitySelectorAccessor;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>通过实体选择器实现的实体谓词。在测试时，如果对应的实体选择器没有指定数量，则会根据实体选择器内的一些属性来对实体进行判断，包括判断实体是否为命令的指靠者、实体是否为玩家等。如果实体选择器限制了实体的数量，那么会先选择出这些数量的实体，然后再判断指定的实体是否属于被选择出来的这些实体。
 * <p>实体选择器在创建时，就会直接通过 {@link #getSpecialEntries(EntitySelector, ServerCommandSource)} 计算出具体的、可直接用于判断的谓词。该谓词会在构造函数中直接计算出来，无需手动提供。
 * <p>此对象会包含一个 {@link ServerCommandSource} 对象。
 * <p>在创建了此对象之后，就不要再对 {@link EntitySelector} 进行后续的更改。
 */
public class SelectorEntityPredicate implements EntityPredicate {
  /**
   * 该实体谓词所基于的实体选择器。
   */
  public final EntitySelector entitySelector;
  /**
   * 该实体选择实际用于判断实体的谓词对象。
   */
  public final List<SpecialEntityPredicate> specialEntries;
  /**
   * 直接由实体选择器参数指定，且与 {@code serverCommandSource} 无关的谓词。
   */
  public final List<EntityPredicateEntry> standardEntries;
  /**
   * 由 {@link EntitySelectorExtras#predicateFunctions} 根据 {@link ServerCommandSource} 计算出的实体谓词。
   */
  public final List<EntityPredicateEntry> sourcedEntries;
  protected final ServerCommandSource source;
  protected final @Nullable Collection<? extends Entity> limitedEntities;

  public SelectorEntityPredicate(EntitySelector entitySelector, ServerCommandSource source) throws CommandSyntaxException {
    this.entitySelector = entitySelector;
    this.specialEntries = getSpecialEntries(entitySelector, source);
    this.standardEntries = ((EntitySelectorAccessor) entitySelector).getPredicates().stream().map(predicate -> predicate instanceof EntityPredicateEntry entry ? entry : new UnknownEntityPredicateEntry(predicate)).collect(ImmutableList.toImmutableList());
    final var predicateFunctions = entitySelector.extension$ec().predicateFunctions;
    this.sourcedEntries = predicateFunctions == null ? ImmutableList.of() : IterateUtils.transformFailableImmutableList(predicateFunctions, f -> f.apply(source));
    this.source = source;
    if (entitySelector.getLimit() < Integer.MAX_VALUE) {
      this.limitedEntities = entitySelector.getEntities(source.hasPermissionLevel(2) ? source : source.withLevel(2));
    } else {
      this.limitedEntities = null;
    }
  }

  /**
   * 将实体选择器转换为谓词（非 {@link EntityPredicate} 对象。考虑到选择器中会有一些依赖 {@link ServerCommandSource} 的地方，因此需要提供 {@link ServerCommandSource}。
   */
  public static List<SpecialEntityPredicate> getSpecialEntries(EntitySelector entitySelector, ServerCommandSource source) throws CommandSyntaxException {
    final List<SpecialEntityPredicate> entries = new ArrayList<>();
    entitySelector.extension$ec().updateSource(source);
    final var accessor = (EntitySelectorAccessor) entitySelector;

    if (!entitySelector.includesNonPlayers()) {
      entries.add(PlayerOnlyEntityPredicate.INSTANCE);
    }
    if (accessor.getPlayerName() != null) {
      entries.add(new PlayerNameEntityPredicate(accessor.getPlayerName()));
    }
    if (accessor.getUuid() != null) {
      entries.add(new UuidEntityPredicateEntry(accessor.getUuid()));
    }
    if (entitySelector.isSenderOnly()) {
      entries.add(new SenderOnlyEntityPredicate(source.getEntity()));
    }
    if (entitySelector.isLocalWorldOnly()) {
      entries.add(new LocalWorldOnlyEntityPredicate(source.getWorld()));
    }

    final Vec3d vec3d = accessor.getPositionOffset().apply(source.getPosition());

    final FeatureSet enabledFeatures = (entitySelector.isSenderOnly() || !entitySelector.includesNonPlayers()) ? null : source.getEnabledFeatures();
    if (enabledFeatures != null) {
      entries.add(new EnabledFeaturesEntityPredicate(enabledFeatures));
    }

    final Box box = accessor.callGetOffsetBox(vec3d);
    if (box != null) {
      entries.add(new BoxEntityPredicate(box));
    }

    if (!accessor.getDistance().isDummy()) {
      entries.add(new DistanceEntityPredicate(accessor.getDistance(), vec3d));
    }

    return entries;
  }

  @Override
  public boolean test(@NotNull Entity entity) {
    if (limitedEntities != null) {
      return limitedEntities.contains(entity);
    } else {
      for (EntityPredicate predicate : Iterables.concat(specialEntries, standardEntries, sourcedEntries)) {
        if (!predicate.test(entity)) {
          return false;
        }
      }
      return true;
    }
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, Text displayName) throws CommandSyntaxException {
    List<TestResult> descriptions = new ArrayList<>();

    boolean result = true;
    for (EntityPredicate predicate : Iterables.concat(specialEntries, standardEntries, sourcedEntries)) {
      final TestResult e = predicate.testAndDescribe(entity, displayName);
      descriptions.add(e);
      result = result && e.successes();
    }

    if (descriptions.isEmpty()) {
      return EntityPredicate.successOrFail(result, entity);
    } else {
      return TestResult.of(result, result ? Text.translatable("enhanced_commands.entity_predicate.pass_selector", displayName) : Text.translatable("enhanced_commands.entity_predicate.fail_selector", displayName), descriptions);
    }
  }
}
