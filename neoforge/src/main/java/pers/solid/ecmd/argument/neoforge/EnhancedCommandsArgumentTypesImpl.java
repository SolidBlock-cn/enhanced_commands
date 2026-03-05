package pers.solid.ecmd.argument.neoforge;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import pers.solid.ecmd.EnhancedCommands;

public class EnhancedCommandsArgumentTypesImpl {
  public static final DeferredRegister<ArgumentTypeInfo<?, ?>> DEFERRED_REGISTER = DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, EnhancedCommands.MOD_ID);

  public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void register(String name, Class<A> clazz, ArgumentTypeInfo<A, T> info) {
    ArgumentTypeInfos.registerByClass(clazz, info);
    DEFERRED_REGISTER.register(name, () -> info);
  }
}
