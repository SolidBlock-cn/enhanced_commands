package pers.solid.mod.argument;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
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
    ArgumentTypeRegistry.registerArgumentType(new Identifier(EnhancedCommands.MOD_ID, "block_predicate"), BlockPredicateArgumentType.class, ConstantArgumentSerializer.of(BlockPredicateArgumentType::new));
    ArgumentTypeRegistry.registerArgumentType(new Identifier(EnhancedCommands.MOD_ID, "direction"), DirectionArgumentType.class, ConstantArgumentSerializer.of(DirectionArgumentType::create));
  }
}
