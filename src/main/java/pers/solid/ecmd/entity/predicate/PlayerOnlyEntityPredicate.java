package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

/**
 * 此为技术性的实体谓词，主要用于部分实体选择器因为限定游戏模式等原因只接受玩家等情形。
 */
public enum PlayerOnlyEntityPredicate implements SpecialEntityPredicate, StaticEntityPredicate {
  INSTANCE;
  public static final MapCodec<PlayerOnlyEntityPredicate> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(Entity entity) {
    return entity.isAlwaysTicking();
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final boolean isPlayer = entity.isAlwaysTicking();
    if (isPlayer) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.player.true", displayName));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.player.false", displayName));
    }
  }

  @Override
  public EntityPredicateType<PlayerOnlyEntityPredicate> getType() {
    return EntityPredicateTypes.PLAYER_ONLY;
  }

  @Override
  public String asString() {
    return "<player only>";
  }
}
