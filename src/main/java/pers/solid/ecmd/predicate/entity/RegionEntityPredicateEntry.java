package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.TestResult;

public record RegionEntityPredicateEntry(@NotNull Region region) implements EntityPredicateEntry {
  @Override
  public @Nullable String toOptionEntry() {
    return "region=" + region.asString();
  }

  @Override
  public boolean test(@NotNull Entity entity) {
    return region.contains(entity.getPos());
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    return null;
  }
}
