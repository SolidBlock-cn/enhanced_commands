package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public enum AliveEntityPredicate implements StaticEntityPredicate, EntityPredicateEntry {
  INSTANCE;
  public static final MapCodec<AliveEntityPredicate> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(Entity entity) {
    return entity.isAlive();
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final boolean alive = entity.isAlive();
    if (alive) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.alive.true", displayName));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.alive.false", displayName));
    }
  }

  @Override
  public @NotNull EntityPredicateType<AliveEntityPredicate> getType() {
    return EntityPredicateTypes.ALIVE;
  }

  @Override
  public @Nullable String toOptionEntry() {
    return null;
  }

  @Override
  public @NotNull String asString() {
    return "@e";
  }
}
