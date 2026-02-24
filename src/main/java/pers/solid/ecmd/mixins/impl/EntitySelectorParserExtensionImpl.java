package pers.solid.ecmd.mixins.impl;

import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.mixins.ext.EntitySelectorParserExtension;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;

@Mixin(EntitySelectorParser.class)
public abstract class EntitySelectorParserExtensionImpl implements EntitySelectorParserExtension {
  @Unique
  private final EntitySelectorReaderExtras extension$ec = new EntitySelectorReaderExtras((EntitySelectorParser) (Object) this);

  @Override
  public EntitySelectorReaderExtras extension$ec() {
    return extension$ec;
  }
}
