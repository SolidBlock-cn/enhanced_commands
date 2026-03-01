package pers.solid.ecmd.api;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;

public interface ClientCommandRegistrationCallbackBridge {
  <S> void register(CommandDispatcher<S> commandDispatcher, CommandBuildContext commandRegistryAccess);
}
