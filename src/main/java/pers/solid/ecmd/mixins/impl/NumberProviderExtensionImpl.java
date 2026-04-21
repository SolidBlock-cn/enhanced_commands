package pers.solid.ecmd.mixins.impl;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

@Mixin(NumberProvider.class)
public interface NumberProviderExtensionImpl extends NumberProviderExtension {
  @Shadow
  float getFloat(LootContext lootContext);

  @Shadow
  int getInt(LootContext lootContext);

  default float getFloat(ExecutionContext executionContext) {
    return getFloat(executionContext.lootContext());
  }

  default int getInt(ExecutionContext executionContext) {
    return getInt(executionContext.lootContext());
  }
}
