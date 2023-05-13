package pers.solid.mod.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import pers.solid.mod.EnhancedCommands;
import pers.solid.mod.predicate.block.BlockPredicateTypes;

/**
 * @see net.minecraft.command.argument.ArgumentTypes
 */
public class ModArgumentTypes {
  public static void init() {
    BlockPredicateTypes.init();
    register("block_predicate", BlockPredicateArgumentType.class, ConstantArgumentSerializer.of(BlockPredicateArgumentType::new));
    register("directions", DirectionArgumentType.class, ConstantArgumentSerializer.of(DirectionArgumentType::create));
  }

  private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void register(
      String name, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(new Identifier(EnhancedCommands.MOD_ID, name), clazz, serializer);
  }
}
