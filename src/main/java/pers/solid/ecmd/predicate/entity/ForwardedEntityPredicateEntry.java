package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.TestResult;

import java.util.function.Predicate;

public record ForwardedEntityPredicateEntry(Predicate<Entity> vanillaPredicate, EntityPredicateEntry forward) implements EntityPredicateEntry {
  @Override
  public @Nullable String toOptionEntry() {
    return forward.toOptionEntry();
  }

  @Override
  public boolean test(@NotNull Entity entity) {
    return vanillaPredicate.test(entity);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    return forward.testAndDescribe(entity, displayName);
  }
}
