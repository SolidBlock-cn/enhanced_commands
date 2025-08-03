package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;

public enum EmptyEntityPredicateEntry implements EntityPredicateEntry, StaticEntityPredicate {
  INSTANCE;
  public static final MapCodec<EmptyEntityPredicateEntry> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(@NotNull Entity entity) {
    return false;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.empty"));
  }

  @Override
  public @NotNull EntityPredicateType<EmptyEntityPredicateEntry> getType() {
    return EntityPredicateTypes.EMPTY;
  }

  @Override
  public String toOptionEntry() {
    return null;
  }
}
