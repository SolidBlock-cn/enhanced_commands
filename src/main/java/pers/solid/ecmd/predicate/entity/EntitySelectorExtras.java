package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.mixin.EntitySelectorExtension;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class EntitySelectorExtras {
  public ServerCommandSource source;
  public @Nullable List<Function<ServerCommandSource, Predicate<Entity>>> predicateFunctions = null;
  public @Nullable List<Function<ServerCommandSource, EntityPredicateEntry>> predicateDescriptions = null;
  public Predicate<Entity> actualExtraPredicate = entity -> {
    EnhancedCommands.LOGGER.warn("Warning! There is no ServerCommandSource yet for {}!", EntitySelectorExtras.this);
    return false;
  };

  public Predicate<Entity> createUpdatedPredicate(ServerCommandSource source) {
    // 这个 transform 过的 iterable 会被复制一遍。
    return predicateFunctions == null ? Predicates.alwaysTrue() : Predicates.and(Iterables.transform(predicateFunctions, predicateFunction -> predicateFunction.apply(source)::test));
  }

  public void updateSource(@NotNull ServerCommandSource source) {
    if (!source.equals(this.source)) {
      this.source = source;
      actualExtraPredicate = createUpdatedPredicate(source);
    }
  }

  public static EntitySelectorExtras getOf(EntitySelector entitySelector) {
    return ((EntitySelectorExtension) entitySelector).ec$getExt();
  }

  public boolean testForExtraPredicates(Entity entity) {
    return actualExtraPredicate.test(entity);
  }
}
