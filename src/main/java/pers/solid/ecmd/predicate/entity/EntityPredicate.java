package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.entity.Entity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.text.Text;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.*;

/**
 * <p>实体谓词，用于判断某个实体是否符合指定的条件，同时还对其判断过程进行描述。
 * <p>实体谓词是对{@linkplain net.minecraft.command.EntitySelector 实体选择器}的扩展，会直接判断实体是否符合此条件，而不需要将实体选择器那样先选择出符合条件的实体。在一些情况下，实体谓词有些类似于 {@link LootCondition}。借助实体选择器实现的实体谓词是 {@link SelectorEntityPredicate}。
 *
 * @see net.minecraft.predicate.entity.EntityPredicate
 * @see net.minecraft.loot.condition.EntityPropertiesLootCondition
 */
public interface EntityPredicate extends ExpressionConvertible {
  Codec<EntityPredicate> CODEC = EntityPredicateType.REGISTRY.getCodec().dispatch(EntityPredicate::getType, EntityPredicateType::codec);

  /**
   * 测试实体是否符合条件。
   *
   * @param entity 被测试的实体
   * @return 如果实体满足条件，则为 {@code true}
   */
  boolean test(@NotNull Entity entity, @NotNull ExecutionContext context);

  static TestResult successResult(@NotNull Entity entity) {
    return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.pass", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)));
  }

  static TestResult failResult(@NotNull Entity entity) {
    return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.fail", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)));
  }

  static TestResult successOrFail(boolean successes, @NotNull Entity entity) {
    return successes ? successResult(entity) : failResult(entity);
  }

  /**
   * 测试实体并返回描述信息。调用时请使用此类，但覆盖时请覆盖 {@link #testAndDescribe(Entity, ExecutionContext, Text)}。
   */
  @ApiStatus.NonExtendable
  default TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context) throws CommandSyntaxException {
    return testAndDescribe(entity, context, TextUtil.styled(entity.getDisplayName(), Styles.TARGET));
  }

  /**
   * 测试实体并返回描述信息，实现接口应覆盖此方法，但通常不要直接调用此方法，但是如果需要对同一个实体多次调用此方法，则可以使用此方法并共用 {@code displayName} 参数。使用 {@code displayName} 是考虑到其会被多次用到，为了避免多次创建其对象而直接使用共用的此对象。
   *
   * @param entity      被测试的实体。
   * @param context
   * @param displayName 被测试的实体的显示名称。
   */
  TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException;

  @NotNull EntityPredicateType<?> getType();
}
