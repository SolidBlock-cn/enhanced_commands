package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public enum AliveEntityPredicate implements StaticEntityPredicate, EntityPredicateEntry, DoesNotRequireValidation {
  INSTANCE;
  public static final MapCodec<AliveEntityPredicate> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(Entity entity) {
    return entity.isAlive();
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final boolean alive = entity.isAlive();
    if (alive) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.alive.true", displayName));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.alive.false", displayName));
    }
  }

  @Override
  public EntityPredicateType<AliveEntityPredicate> getType() {
    return EntityPredicateTypes.ALIVE;
  }

  @Override
  public @Nullable String toOptionEntry() {
    return null;
  }

  @Override
  public String expressAsString() {
    return "@e";
  }
}
