package pers.solid.ecmd.mixins.impl;

import net.minecraft.command.EntitySelectorReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.mixins.ext.EntitySelectorReaderExtension;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;

@Mixin(EntitySelectorReader.class)
public abstract class EntitySelectorReaderExtensionImpl implements EntitySelectorReaderExtension {
  @Unique
  private final EntitySelectorReaderExtras extension$ec = new EntitySelectorReaderExtras((EntitySelectorReader) (Object) this);

  @Override
  public EntitySelectorReaderExtras extension$ec() {
    return extension$ec;
  }
}
