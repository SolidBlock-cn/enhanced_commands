package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

import java.util.UUID;

/**
 * 主要用于没有使用实体选择器参数而是直接指定 id 的情形。
 *
 * @param uuid
 */
public record UuidEntityPredicateEntry(@NotNull UUID uuid) implements SpecialEntityPredicate, StaticEntityPredicate {
  public static final MapCodec<UuidEntityPredicateEntry> CODEC = Uuids.CODEC.fieldOf("uuid").xmap(UuidEntityPredicateEntry::new, UuidEntityPredicateEntry::uuid);

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity.getUuid().equals(uuid);
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final UUID actual = entity.getUuid();
    if (uuid.equals(actual)) {
      return (TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.uuid.true", displayName, actual)));
    } else {
      return (TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.uuid.false", displayName, Text.literal(actual.toString()).styled(Styles.ACTUAL), Text.literal(uuid.toString()).styled(Styles.EXPECTED))));
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
