package pers.solid.ecmd.mixins.impl;

import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.mixins.ext.EntitySelectorExtension;
import pers.solid.ecmd.predicate.entity.EntitySelectorExtras;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorExtensionImpl implements EntitySelectorExtension {
  @Unique
  private final EntitySelectorExtras extension$ec = new EntitySelectorExtras((EntitySelector) (Object) this);

  @Override
  public EntitySelectorExtras extension$ec() {
    return extension$ec;
  }
}
