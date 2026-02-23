package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

import java.util.List;
import java.util.Optional;
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
 * @param owner    适用于该实体的驯养者的谓词，如果为 {@code null}，则判断该实体是否无法驯养或者没有主人。
 * @param inverted 反向该谓词。如果 {@code entityPredicate} 不为 {@code null}，那么当实体没有主人或者无法驯养时，无法是否为 {@code inverted}，均无法通过。
 */
public record OwnerEntityPredicateEntry(@Nullable EntityPredicate owner, boolean inverted) implements EntityPredicateEntry {
  public static final MapCodec<OwnerEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EntityPredicate.CODEC.optionalFieldOf("owner").forGetter(entry -> Optional.ofNullable(entry.owner)),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(OwnerEntityPredicateEntry::inverted)
  ).apply(i, (owner, inverted) -> new OwnerEntityPredicateEntry(owner.orElse(null), inverted)));

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    if (owner == null) {
      return (entity instanceof OwnableEntity tameable && tameable.getOwnerUUID() != null) == inverted;
    } else {
      return entity instanceof OwnableEntity tameable && tameable.getOwner() != null && owner.test(tameable.getOwner(), context) != inverted;
    }
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) throws CommandSyntaxException {
    if (entity instanceof OwnableEntity tameable) {
      final LivingEntity actualOwner = tameable.getOwner();
      if (owner == null) {
        final UUID ownerUuid = tameable.getOwnerUUID();
        boolean hasOwner = ownerUuid != null;
        if (hasOwner) {
          return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.owner.has_owner", displayName, actualOwner == null ? Component.literal(ownerUuid.toString()) : actualOwner.getDisplayName()));
        } else {
          return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.owner.no_owner", displayName));
        }
      }
      if (actualOwner == null) {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.owner.no_owner", displayName));
      }
      final Component ownerDisplayName = actualOwner.getDisplayName();
      final TestResult ownerResult = owner.testAndDescribe(actualOwner, context);
      if (ownerResult.successes()) {
        if (inverted) {
          return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.owner.fail_true", displayName, ownerDisplayName), List.of(ownerResult));
        } else {
          return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.owner.pass_true", displayName, ownerDisplayName), List.of(ownerResult));
        }
      } else {
        if (inverted) {
          return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.owner.pass_false", displayName, ownerDisplayName), List.of(ownerResult));
        } else {
          return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.owner.fail_false", displayName, ownerDisplayName), List.of(ownerResult));
        }
      }
    } else {
      if (owner == null) {
        return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.owner.not_tameable", displayName));
      }
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.owner.not_tameable", displayName));
    }
  }

  @Override
  public @NotNull EntityPredicateType<OwnerEntityPredicateEntry> getType() {
    return EntityPredicateTypes.OWNER;
  }

  @Override
  public String toOptionEntry() {
    return "owner=" + (inverted ? "!" : "") + (owner == null ? "" : owner.asString());
  }
}
