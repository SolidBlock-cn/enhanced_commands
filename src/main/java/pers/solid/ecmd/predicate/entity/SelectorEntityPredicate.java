package pers.solid.ecmd.predicate.entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>通过实体选择器实现的实体谓词。在测试时，如果对应的实体选择器没有指定数量，则会根据实体选择器内的一些属性来对实体进行判断，包括判断实体是否为命令的执行者、实体是否为玩家等。如果实体选择器限制了实体的数量，那么会先选择出这些数量的实体，然后再判断指定的实体是否属于被选择出来的这些实体。
 * <p>此对象会包含一个 {@link CommandSourceStack} 对象。
 * <p>在创建了此对象之后，就不要再对 {@link EntitySelector} 进行后续的更改。
 *
 * @param entitySelector 该实体谓词所基于的实体选择器。
 */
public record SelectorEntityPredicate(EntitySelector entitySelector) implements EntityPredicate {
  public static final MapCodec<SelectorEntityPredicate> CODEC = EntitySelectorCodec.INSTANCE.xmap(SelectorEntityPredicate::new, selectorEntityPredicate -> selectorEntityPredicate.entitySelector);
  private static final LoadingCache<@NotNull EntitySelector, LoadingCache<@NotNull ExecutionContext, Set<Entity>>> CACHE = CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(selector -> CacheBuilder.newBuilder().weakKeys().weakValues().build(CacheLoader.from(context -> {
    try {
      return Set.copyOf(selector.findEntities(((CommandSourceStack) context.positionProvider)));
    } catch (CommandSyntaxException e) {
      throw new CommandRuntimeException(e);
    }
  }))));

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    if (entitySelector.getMaxResults() < Integer.MAX_VALUE) {
      return CACHE.getUnchecked(entitySelector).getUnchecked(context).contains(entity);
    } else {
      final Iterable<@NotNull EntityPredicate> concat = Iterables.concat(entitySelector.extension$ec().getSpecialEntries(), entitySelector.extension$ec().getStandardPredicates());
      for (EntityPredicate predicate : concat) {
        if (!predicate.test(entity, context)) {
          return false;
        }
      }
      return true;
    }
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) throws CommandSyntaxException {
    List<TestResult> descriptions = new ArrayList<>();

    boolean result = true;
    if (entitySelector.getMaxResults() < Integer.MAX_VALUE) {
      result = CACHE.getUnchecked(entitySelector).getUnchecked(context).contains(entity);
    }
    for (EntityPredicate predicate : Iterables.concat(entitySelector.extension$ec().getSpecialEntries(), entitySelector.extension$ec().getStandardPredicates())) {
      final TestResult e = predicate.testAndDescribe(entity, context, displayName);
      descriptions.add(e);
      result = result && e.successes();
    }

    if (descriptions.isEmpty()) {
      return EntityPredicate.successOrFail(result, entity);
    } else {
      return TestResult.of(result, result ? Component.translatable("enhanced_commands.entity_predicate.pass_selector", displayName) : Component.translatable("enhanced_commands.entity_predicate.fail_selector", displayName), descriptions);
    }
  }

  @Override
  public @NotNull EntityPredicateType<SelectorEntityPredicate> getType() {
    return EntityPredicateTypes.SELECTOR;
  }

  @Override
  public @NotNull String asString() {
    return EntitySelectors.express(entitySelector);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SelectorEntityPredicate that)) return false;

    return EntitySelectors.equals(entitySelector, that.entitySelector);
  }

  @Override
  public int hashCode() {
    return EntitySelectors.hashCode(entitySelector);
  }
}
