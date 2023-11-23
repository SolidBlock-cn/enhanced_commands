package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import pers.solid.ecmd.command.TestResult;

public interface EntityPredicateEntry {
  TestResult testAndDescribe(Entity entity);

  String toOptionEntry();
}
