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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.mixins.mixin.EntitySelectorReaderMixin;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.List;
import java.util.function.Predicate;

public class EntitySelectorExtras {
  public static final Logger LOGGER = LoggerFactory.getLogger(EntitySelectorExtras.class);
  public ServerCommandSource source;
  public @Nullable List<FailableFunction<ServerCommandSource, EntityPredicateEntry, CommandSyntaxException>> predicateFunctions = null;
  public Predicate<Entity> actualExtraPredicate = entity -> {
    EnhancedCommands.LOGGER.warn("Warning! There is no ServerCommandSource yet for {}!", EntitySelectorExtras.this);
    return false;
  };
  /**
   * 此字体决定了在运行 {@link EntitySelector#getEntities(ServerCommandSource)} 和 {@link EntitySelector#getPlayers(ServerCommandSource)} 时，如何以特殊的方式收集实体。
   *
   * @see EntitySelectorReaderMixin#buildExtraPredicate(CallbackInfoReturnable)
   */
  public @Nullable EntitySelectorCollector collector;

  public Predicate<Entity> createUpdatedPredicate(ServerCommandSource source) throws CommandSyntaxException {
    // 这个 transform 过的 iterable 会被复制一遍。
    return predicateFunctions == null ? Predicates.alwaysTrue() : Predicates.and(IterateUtils.transformFailableImmutableList(predicateFunctions, predicateFunction -> predicateFunction.apply(source)::test));
  }

  public void updateSource(@NotNull ServerCommandSource source) throws CommandSyntaxException {
    if (!source.equals(this.source)) {
      // todo is it safe to change to another source?
      this.source = source;
      actualExtraPredicate = createUpdatedPredicate(source);
    }
  }

  /**
   * 获取已有 {@link EntitySelector} 中的 {@link EntitySelectorExtras} 对象。当接口没有注入或者无法编译时，可以调用此方法。
   */
  public static EntitySelectorExtras getOf(EntitySelector entitySelector) {
    return entitySelector.extension$ec();
  }

  public boolean testForExtraPredicates(Entity entity) {
    return actualExtraPredicate.test(entity);
  }
}
