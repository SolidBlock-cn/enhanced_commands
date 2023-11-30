package pers.solid.ecmd.predicate.entity;

import com.google.common.base.Predicates;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.EntitySelectorExtension;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class EntitySelectorExtras {
  public static final Logger LOGGER = LoggerFactory.getLogger(EntitySelectorExtras.class);
  public ServerCommandSource source;
  public @Nullable List<FailableFunction<ServerCommandSource, Predicate<Entity>, CommandSyntaxException>> predicateFunctions = null;
  public @Nullable List<Function<ServerCommandSource, EntityPredicateEntry>> predicateDescriptions = null;
  public Predicate<Entity> actualExtraPredicate = entity -> {
    EnhancedCommands.LOGGER.warn("Warning! There is no ServerCommandSource yet for {}!", EntitySelectorExtras.this);
    return false;
  };

  public Predicate<Entity> createUpdatedPredicate(ServerCommandSource source) throws CommandSyntaxException {
    // 这个 transform 过的 iterable 会被复制一遍。
    return predicateFunctions == null ? Predicates.alwaysTrue() : Predicates.and(IterateUtils.transformFailableImmutableList(predicateFunctions, predicateFunction -> predicateFunction.apply(source)::test));
  }

  public void updateSource(@NotNull ServerCommandSource source) throws CommandSyntaxException {
    if (!source.equals(this.source)) {
      if (this.source != null) {
        LOGGER.warn("Changing source for a same entity selector object ({}) from {} to {}!", this, this.source, source);
      }
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
