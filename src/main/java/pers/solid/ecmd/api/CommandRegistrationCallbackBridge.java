package pers.solid.ecmd.api;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * 用于 Fabric 的 {@code CommandRegistrationCallback}
 */
public interface CommandRegistrationCallbackBridge {
  void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment);
}
