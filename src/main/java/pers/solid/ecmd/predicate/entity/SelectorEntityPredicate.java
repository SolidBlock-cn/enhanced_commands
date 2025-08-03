package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>通过实体选择器实现的实体谓词。在测试时，如果对应的实体选择器没有指定数量，则会根据实体选择器内的一些属性来对实体进行判断，包括判断实体是否为命令的指靠者、实体是否为玩家等。如果实体选择器限制了实体的数量，那么会先选择出这些数量的实体，然后再判断指定的实体是否属于被选择出来的这些实体。
 * <p>此对象会包含一个 {@link ServerCommandSource} 对象。
 * <p>在创建了此对象之后，就不要再对 {@link EntitySelector} 进行后续的更改。
 */
public class SelectorEntityPredicate implements EntityPredicate {
  public static final MapCodec<SelectorEntityPredicate> CODEC = EntitySelectorCodec.INSTANCE.fieldOf("selector").xmap(SelectorEntityPredicate::new, selectorEntityPredicate -> selectorEntityPredicate.entitySelector);
  /**
   * 该实体谓词所基于的实体选择器。
   */
  public final EntitySelector entitySelector;

  protected @Nullable Collection<? extends Entity> limitedEntities;

  public SelectorEntityPredicate(EntitySelector entitySelector) {
    this.entitySelector = entitySelector;
  }

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    if (limitedEntities != null) {
      return limitedEntities.contains(entity);
    } else {
      for (EntityPredicate predicate : Iterables.concat(entitySelector.extension$ec().getSpecialEntries(), entitySelector.extension$ec().getStandardPredicates())) {
        if (!predicate.test(entity, context)) {
          return false;
        }
      }
      return true;
    }
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    List<TestResult> descriptions = new ArrayList<>();

    boolean result = true;
    for (EntityPredicate predicate : Iterables.concat(entitySelector.extension$ec().getSpecialEntries(), entitySelector.extension$ec().getStandardPredicates())) {
      final TestResult e = predicate.testAndDescribe(entity, context, displayName);
      descriptions.add(e);
      result = result && e.successes();
    }

    if (descriptions.isEmpty()) {
      return EntityPredicate.successOrFail(result, entity);
    } else {
      return TestResult.of(result, result ? Text.translatable("enhanced_commands.entity_predicate.pass_selector", displayName) : Text.translatable("enhanced_commands.entity_predicate.fail_selector", displayName), descriptions);
    }
  }

  @Override
  public @NotNull EntityPredicateType<SelectorEntityPredicate> getType() {
    return EntityPredicateTypes.SELECTOR;
  }

  @Override
  public @NotNull String asString() {
    return EntitySelectorHelper.express(entitySelector);
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof SelectorEntityPredicate that)) return false;

    return EntitySelectorHelper.equals(entitySelector, that.entitySelector) && Objects.equals(limitedEntities, that.limitedEntities);
  }

  @Override
  public int hashCode() {
    int result = EntitySelectorHelper.hashCode(entitySelector);
    result = 31 * result + Objects.hashCode(limitedEntities);
    return result;
  }
}
