package pers.solid.ecmd.entity.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.Collections;
import java.util.List;

/**
 * <p>此实体谓词用于要求实体选择器同时符合另一个或者不符合另一个指定的谓词。例如：
 * <ul>
 *   <li>{@code @p[is=@s]}：既是距离最近又是实体自身。</li>
 *   <li>{@code @e[not=@s]}：所有实体是不是自身的实体。</li>
 * </yl>
 * <p>此选择可以多次重复使用。</p>
 *
 * @param predicate 使用的另一个实体谓词
 * @param inverted  是否反转条件，如果为 {@code true}，则只有当指定的谓词不通过时，整个谓词才能通过
 */
public record SubPredicateEntityPredicateEntry(EntityPredicate predicate, boolean inverted) implements EntityPredicateEntry {
  public static final MapCodec<SubPredicateEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EntityPredicate.CODEC.fieldOf("predicate").forGetter(SubPredicateEntityPredicateEntry::predicate),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(SubPredicateEntityPredicateEntry::inverted)
  ).apply(i, SubPredicateEntityPredicateEntry::new));

  @Override
  public String toOptionEntry() {
    return (inverted ? "not=" : "is=") + predicate.expressAsString();
  }

  @Override
  public boolean test(Entity entity, ExecutionContext context) {
    return predicate.test(entity, context) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) throws CommandSyntaxException {
    final TestResult testResult = predicate.testAndDescribe(entity, context, displayName);
    if (inverted) {
      if (testResult.successes()) {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.sub_predicate.fail_inverted", displayName), Collections.singletonList(testResult));
      } else {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.sub_predicate.pass_inverted", displayName), Collections.singletonList(testResult));
      }
    } else {
      if (testResult.successes()) {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.sub_predicate.pass", displayName), Collections.singletonList(testResult));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.sub_predicate.fail", displayName), Collections.singletonList(testResult));
      }
    }
  }

  @Override
  public EntityPredicateType<SubPredicateEntityPredicateEntry> getType() {
    return EntityPredicateTypes.SUB_PREDICATE;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(predicate);
  }
}
