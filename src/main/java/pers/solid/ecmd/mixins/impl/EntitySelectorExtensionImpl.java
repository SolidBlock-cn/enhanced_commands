package pers.solid.ecmd.mixins.impl;

import net.minecraft.command.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.mixins.ext.EntitySelectorExtension;
import pers.solid.ecmd.predicate.entity.EntitySelectorExtras;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorExtensionImpl implements EntitySelectorExtension {
  @Unique
  private final EntitySelectorExtras extension$ec = new EntitySelectorExtras();

  @Override
  public EntitySelectorExtras extension$ec() {
    return extension$ec;
  }
}
