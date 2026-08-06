package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public enum EmptyEntityPredicateEntry implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  INSTANCE;
  public static final MapCodec<EmptyEntityPredicateEntry> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(Entity entity) {
    return false;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.empty"));
  }

  @Override
  public EntityPredicateType<EmptyEntityPredicateEntry> getType() {
    return EntityPredicateTypes.EMPTY;
  }

  @Override
  public String toOptionEntry() {
    return null;
  }
}
