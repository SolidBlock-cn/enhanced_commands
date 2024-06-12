package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TestResult;

import java.util.List;
import java.util.UUID;

/**
 * 检测实体是否被其他实体驯养（即实体是否有主人），以及该实体的驯养者（即该实体的主人）。例如：
 * <ul>
 *   <li>{@code owner=<实体谓词>} - 实体被驯养，且驯养者符合该谓词</li>
 *   <li>{@code owner=!<实体谓词}> - 实体被驯养，且驯养者不符合该谓词（如果实体未被驯养，或不可驯养，则仍不通过）</li>
 *   <li>{@code owner=} - 实体未被驯养，或不可驯养</li>
 *   <li>{@code owner=!} - 实体被任意实体驯养</li>
 * </li>
 *
 * @param entityPredicate 适用于该实体的驯养者的谓词，如果为 {@code null}，则判断该实体是否无法驯养或者没有主人。
 * @param inverted        反向该谓词。如果 {@code entityPredicate} 不为 {@code null}，那么当实体没有主人或者无法驯养时，无法是否为 {@code inverted}，均无法通过。
 */
public record OwnerEntityPredicateEntry(@Nullable EntityPredicate entityPredicate, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    if (entityPredicate == null) {
      return (entity instanceof Tameable tameable && tameable.getOwnerUuid() != null) == inverted;
    } else {
      return entity instanceof Tameable tameable && tameable.getOwner() != null && entityPredicate.test(tameable.getOwner()) != inverted;
    }
  }


  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (entity instanceof Tameable tameable) {
      final LivingEntity owner = tameable.getOwner();
      if (entityPredicate == null) {
        final UUID ownerUuid = tameable.getOwnerUuid();
        boolean hasOwner = ownerUuid != null;
        if (hasOwner) {
          return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.owner.has_owner", displayName, owner == null ? Text.literal(ownerUuid.toString()) : owner.getDisplayName()));
        } else {
          return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.owner.no_owner", displayName));
        }
      }
      if (owner == null) {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.owner.no_owner", displayName));
      }
      final Text ownerDisplayName = owner.getDisplayName();
      final TestResult ownerResult = entityPredicate.testAndDescribe(owner);
      if (ownerResult.successes()) {
        if (inverted) {
          return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.owner.fail_true", displayName, ownerDisplayName), List.of(ownerResult));
        } else {
          return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.owner.pass_true", displayName, ownerDisplayName), List.of(ownerResult));
        }
      } else {
        if (inverted) {
          return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.owner.pass_false", displayName, ownerDisplayName), List.of(ownerResult));
        } else {
          return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.owner.fail_false", displayName, ownerDisplayName), List.of(ownerResult));
        }
      }
    } else {
      if (entityPredicate == null) {
        return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.owner.not_tameable", displayName));
      }
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.owner.not_tameable", displayName));
    }
  }

  @Override
  public String toOptionEntry() {
    return "tamer=" + (inverted ? "!" : "") + (entityPredicate == null ? "" : "[...]");
  }
}
