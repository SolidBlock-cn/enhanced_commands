package pers.solid.ecmd.mixins.accessor;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.ExecuteCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExecuteCommand.class)
public interface ExecuteCommandAccessor2 {
  @Invoker("storeValue")
  static CommandSourceStack callStoreBossbarValue(CommandSourceStack source, CustomBossEvent bossBar, boolean storeInValue, boolean requestResult) {
    throw new UnsupportedOperationException();
  }
}
