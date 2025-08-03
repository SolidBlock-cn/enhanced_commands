package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

/**
 * 此为技术性的实体谓词，主要用于部分实体选择器因为限定游戏模式等原因只接受玩家等情形。
 */
public enum PlayerOnlyEntityPredicate implements SpecialEntityPredicate, StaticEntityPredicate {
  INSTANCE;
  public static final MapCodec<PlayerOnlyEntityPredicate> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity.isPlayer();
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final boolean isPlayer = entity.isPlayer();
    if (isPlayer) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.player.true", displayName));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.player.false", displayName));
    }
  }

  @Override
  public @NotNull EntityPredicateType<PlayerOnlyEntityPredicate> getType() {
    return EntityPredicateTypes.PLAYER_ONLY;
  }

  @Override
  public @NotNull String asString() {
    return "<player only>";
  }
}
