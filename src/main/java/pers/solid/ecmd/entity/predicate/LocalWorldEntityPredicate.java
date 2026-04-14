package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public enum LocalWorldEntityPredicate implements SpecialEntityPredicate {
  INSTANCE;
  public static final MapCodec<LocalWorldEntityPredicate> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(Entity entity, ExecutionContext context) {
    return entity.level().equals(context.positionProvider.getWorld$ec());
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final Level world = entity.level();
    final Level sourceWorld = context.positionProvider.getWorld$ec();
    if (world.equals(sourceWorld)) {
      return (TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.local_world.true", displayName, TextUtil.literal(world.dimension().location()).withStyle(Styles.ACTUAL))));
    } else {
      return (TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.local_world.false", displayName, TextUtil.literal(world.dimension().location()).withStyle(Styles.ACTUAL), Component.literal(sourceWorld == null ? "<unknown>" : sourceWorld.dimension().location().toString()).withStyle(Styles.EXPECTED))));
    }
  }

  @Override
  public EntityPredicateType<LocalWorldEntityPredicate> getType() {
    return EntityPredicateTypes.LOCAL_WORLD;
  }

  @Override
  public String asString() {
    return "<local world>";
  }
}
