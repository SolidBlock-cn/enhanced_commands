package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.accessor.EntitySelectorReaderAccessor;

/**
 * 此类包含了与实体选择器有关的一些实用方法。
 */
public final class EntitySelectors {
  private EntitySelectors() {
  }

  /**
   * 类似于 {@link EntitySelectorReader#read()}，但是允许省略开头的“@e”等变量。
   */
  public static EntitySelector readOmittibleEntitySelector(@NotNull EntitySelectorReader entitySelectorReader) throws CommandSyntaxException {
    final var accessor = (EntitySelectorReaderAccessor) entitySelectorReader;
    final StringReader stringReader = entitySelectorReader.getReader();

    entitySelectorReader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> {
      suggestionsBuilderConsumer.accept(suggestionsBuilder);
      suggestionsBuilder.suggest("[");
      return suggestionsBuilder.buildFuture();
    });
    if (stringReader.canRead() && stringReader.peek() == '[') {
      stringReader.skip();
      entitySelectorReader.setIncludesNonPlayers(true);
      entitySelectorReader.setLimit(Integer.MAX_VALUE);
      accessor.callReadArguments();
      ((EntitySelectorReaderAccessor) entitySelectorReader).callBuildPredicate();
      return entitySelectorReader.build();
    } else {
      return entitySelectorReader.read();
    }
  }
}
