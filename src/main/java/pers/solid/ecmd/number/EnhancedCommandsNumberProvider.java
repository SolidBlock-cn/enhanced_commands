package pers.solid.ecmd.number;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

public interface EnhancedCommandsNumberProvider extends NumberProvider, NumberProviderExtension {
  @Override
  default float getFloat(LootContext lootContext) {
    return getFloat(ExecutionContext.fromLootContext(lootContext));
  }
}
