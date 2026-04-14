package pers.solid.ecmd.entity.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

import java.util.UUID;

/**
 * 主要用于没有使用实体选择器参数而是直接指定 uuid 的情形。
 */
public record UuidEntityPredicateEntry(@NotNull UUID uuid) implements SpecialEntityPredicate, StaticEntityPredicate {
  public static final MapCodec<UuidEntityPredicateEntry> CODEC = UUIDUtil.AUTHLIB_CODEC.fieldOf("uuid").xmap(UuidEntityPredicateEntry::new, UuidEntityPredicateEntry::uuid);

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity.getUUID().equals(uuid);
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) throws CommandSyntaxException {
    final UUID actual = entity.getUUID();
    if (uuid.equals(actual)) {
      return (TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.uuid.true", displayName, actual)));
    } else {
      return (TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.uuid.false", displayName, Component.literal(actual.toString()).withStyle(Styles.ACTUAL), Component.literal(uuid.toString()).withStyle(Styles.EXPECTED))));
    }
  }

  @Override
  public @NotNull EntityPredicateType<UuidEntityPredicateEntry> getType() {
    return EntityPredicateTypes.UUID;
  }

  @Override
  public @NotNull String asString() {
    return uuid.toString();
  }
}
