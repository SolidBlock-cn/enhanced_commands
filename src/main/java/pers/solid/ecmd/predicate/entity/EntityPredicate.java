package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Predicate;

/**
 * <p>实体谓词，用于判断某个实体是否符合指定的条件，同时还对其判断过程进行描述。
 * <p>类似于 {@link BlockPredicate}，实体谓词在命令中会先解析成 {@link EntityPredicateArgument}，再根据具体的 {@link ServerCommandSource} 来转换成具体的谓词，从而进行判断。此外，与 {@link BlockPredicate} 不同，实体谓词还并没有实现与 NBT 的转换。
 * <p>实体谓词是对{@linkplain net.minecraft.command.EntitySelector 实体选择器}的扩展，会直接判断实体是否符合此条件，而不需要将实体选择器那样先选择出符合条件的实体。在一些情况下，实体谓词有些类似于 {@link LootCondition}。借助实体选择器实现的实体谓词是 {@link SelectorEntityPredicate}。
 *
 * @see net.minecraft.predicate.entity.EntityPredicate
 * @see net.minecraft.loot.condition.EntityPropertiesLootCondition
 * @see EntityPredicateArgument
 */
public interface EntityPredicate extends Predicate<Entity> {
  /**
   * 测试实体是否符合条件。
   *
   * @param entity 被测试的实体
   * @return 如果实体满足条件，则为 {@code true}
   */
  @Override
  boolean test(Entity entity);

  static TestResult successResult(Entity entity) {
    return TestResult.of(true, Text.translatable("enhanced_commands.argument.entity_predicate.pass", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET)));
  }

  static TestResult failResult(Entity entity) {
    return TestResult.of(false, Text.translatable("enhanced_commands.argument.entity_predicate.fail", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET)));
  }

  static TestResult successOrFail(boolean successes, Entity entity) {
    return successes ? successResult(entity) : failResult(entity);
  }

  default TestResult testAndDescribe(Entity entity) throws CommandSyntaxException {
    final boolean test = test(entity);
    return successOrFail(test, entity);
  }
}
