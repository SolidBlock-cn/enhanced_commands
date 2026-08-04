package pers.solid.ecmd.entity.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.mixins.accessor.EntitySelectorAccessor;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.pack.RequiresValidation;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * <p>实体谓词，用于判断某个实体是否符合指定的条件，同时还对其判断过程进行描述。
 * <p>实体谓词是对{@linkplain net.minecraft.commands.arguments.selector.EntitySelector 实体选择器}的扩展，会直接判断实体是否符合此条件，而不需要将实体选择器那样先选择出符合条件的实体。在一些情况下，实体谓词有些类似于 {@link LootItemCondition}。借助实体选择器实现的实体谓词是 {@link SelectorEntityPredicate}。
 *
 * @see net.minecraft.advancements.critereon.EntityPredicate
 * @see net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition
 */
public interface EntityPredicate extends ExpressionConvertible, RequiresValidation {
  Codec<EntityPredicate> CODEC = EntityPredicateType.CODEC.dispatch(EntityPredicate::getType, EntityPredicateType::codec);
  ResourceKey<Registry<EntityPredicate>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("entity_predicate"));

  /**
   * 根据实体选择器，返回对应的实体谓词。如果实体选择器只指定了一个谓词，则返回此谓词（也就是将其简化为单个谓词），否则直接返回 {@link SelectorEntityPredicate}。这一功能目前仍不稳定，特别是如果有模组为实体选择器直接添加新的功能，可能会出现问题，但如果其他模组仅为实体选择器添加谓词，通常只会导致这些谓词无法正常序列化，但不会错误简化为单个的谓词。
   */
  static EntityPredicate simplifiedBySelector(EntitySelector selector) {
    final var accessor = (EntitySelectorAccessor) selector;

    if (selector.isWorldLimited() || !selector.includesEntities()) {
      return new SelectorEntityPredicate(selector);
    }

    final String playerName = accessor.getPlayerName();
    final UUID uuid = accessor.getEntityUUID();
    final EntitySelectorCollector collector = selector.extension$ec().collector;
    final boolean hasDistance = !accessor.getRange().isAny();
    final boolean hasBox = accessor.getAabb() != null;
    final boolean senderOnly = selector.isSelfSelector();
    final List<Predicate<Entity>> predicates = accessor.getContextFreePredicates();
    if ((collector != null
        || hasDistance
        || hasBox
        || playerName != null
        || uuid != null
        || senderOnly)) {
      // 如果有任何特殊属性，且特殊属性只有一个，且没有谓词，则直接返回该特殊属性。
      // 如果有不止一个特殊属性，或者既有特殊属性也有谓词，则最后返回selector。
      if (predicates.isEmpty()) {
        if (collector != null && !hasDistance && !hasBox && playerName == null && uuid == null && !senderOnly) {
          return new CollectorEntityPredicate(collector);
        }
        if (playerName != null && collector == null && !hasDistance && !hasBox && uuid == null && !senderOnly) {
          return new PlayerNameEntityPredicate(playerName);
        }
        if (senderOnly && collector == null && !hasDistance && !hasBox && playerName == null && uuid == null) {
          return SenderOnlyEntityPredicate.INSTANCE;
        }
      }
    } else if (predicates.size() == 1) {
      // 如果没有任何特殊属性，且标准谓词只有一个，则返回此标准谓词。
      return EntitySelectors.calculateStandardPredicates(selector).get(0);
    }

    return new SelectorEntityPredicate(selector);
  }

  /**
   * <p>根据 {@link EntitySelectorParser} 解析一个实体谓词，可以省略“at-变量”。
   * <p>注意：如果在实体选择器参数的处理器中读取，不能直接将已有的 {@link EntitySelectorParser} 作为参数，而应该使用同一个 {@link StringReader} 并创建一个新的 {@link EntitySelectorParser}，例如：
   * <p>❎<i>错误写法：</i>
   * <pre>{@code
   * putOption("option-name", reader -> {
   *    final EntityPredicate entityPredicate = EntityPredicate.parse(reader);
   *    ...
   * }, predicate, text);
   * }</pre>
   * <p>✅<i>正确写法：</i>
   * <pre>{@code
   * putOption("option-name", reader -> {
   *    final EntitySelectorReader newReader = new EntitySelectorReader(reader.getReader(), true);
   *    reader.setSuggestionProvider(newReader::listSuggestions);
   *    final EntityPredicate entityPredicate = EntityPredicate.parse(newReader);
   *    ...
   * }, predicate, text);
   * }</pre>
   */
  static EntityPredicate parse(EntitySelectorParser entitySelectorReader) throws CommandSyntaxException {
    return simplifiedBySelector(EntitySelectors.readOmittibleEntitySelector(entitySelectorReader));
  }

  static TestResult successResult(Entity entity) {
    return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.pass", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)));
  }

  static TestResult failResult(Entity entity) {
    return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.fail", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)));
  }

  static TestResult successOrFail(boolean successes, Entity entity) {
    return successes ? successResult(entity) : failResult(entity);
  }

  /**
   * 测试实体是否符合条件。
   *
   * @param entity 被测试的实体
   * @return 如果实体满足条件，则为 {@code true}
   */
  boolean test(Entity entity, ExecutionContext context);

  /**
   * 测试实体并返回描述信息。调用时请使用此类，但覆盖时请覆盖 {@link #testAndDescribe(Entity, ExecutionContext, Component)}。
   */
  @ApiStatus.NonExtendable
  default TestResult testAndDescribe(Entity entity, ExecutionContext context) throws CommandSyntaxException {
    return testAndDescribe(entity, context, TextUtil.styled(entity.getDisplayName(), Styles.TARGET));
  }

  /**
   * 测试实体并返回描述信息，实现接口应覆盖此方法，但通常不要直接调用此方法，但是如果需要对同一个实体多次调用此方法，则可以使用此方法并共用 {@code displayName} 参数。使用 {@code displayName} 是考虑到其会被多次用到，为了避免多次创建其对象而直接使用共用的此对象。
   *
   * @param entity      被测试的实体。
   * @param context     测试实体的环境。
   * @param displayName 被测试的实体的显示名称。
   */
  TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) throws CommandSyntaxException;

  EntityPredicateType<?> getType();

  @Override
  default Iterable<? extends @Nullable Object> membersToValidate() {
    return Collections.emptyList();
  }
}
