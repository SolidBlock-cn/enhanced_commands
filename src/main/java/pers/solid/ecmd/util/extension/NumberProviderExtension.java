package pers.solid.ecmd.util.extension;

import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Objects;

public interface NumberProviderExtension {
  default NumberProvider enhancedCommands$asVanilla() {
    return (NumberProvider) this;
  }

  default float getFloat(ExecutionContext executionContext) {
    return enhancedCommands$asVanilla().getFloat(executionContext.lootContext());
  }

  default int getInt(ExecutionContext executionContext) {
    return enhancedCommands$asVanilla().getInt(executionContext.lootContext());
  }

  default String asString$enhancedCommands() {
    return NumberProviders.CODEC.encodeStart(NbtOps.INSTANCE, enhancedCommands$asVanilla()).result().map(Objects::toString).orElse("<unknown>");
  }
}
