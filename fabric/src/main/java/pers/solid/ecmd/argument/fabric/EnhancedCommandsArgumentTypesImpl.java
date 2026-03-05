package pers.solid.ecmd.argument.fabric;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.EnhancedCommandsArgumentTypes;

/**
 * @see EnhancedCommandsArgumentTypes
 */
public class EnhancedCommandsArgumentTypesImpl {
  public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void register(String name, Class<A> clazz, ArgumentTypeInfo<A, T> info) {
    ArgumentTypeRegistry.registerArgumentType(EnhancedCommands.id(name), clazz, info);
  }
}
