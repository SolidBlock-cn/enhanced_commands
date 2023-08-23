package pers.solid.ecmd.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.function.block.BlockFunctionTypes;
import pers.solid.ecmd.predicate.block.BlockPredicateTypes;
import pers.solid.ecmd.region.RegionTypes;

/**
 * @see net.minecraft.command.argument.ArgumentTypes
 */
public class ModArgumentTypes {
  public static void init() {
    BlockPredicateTypes.init();
    BlockFunctionTypes.init();
    register("block_predicate", BlockPredicateArgumentType.class, ConstantArgumentSerializer.of(BlockPredicateArgumentType::new));
    register("block_function", BlockFunctionArgumentType.class, ConstantArgumentSerializer.of(BlockFunctionArgumentType::new));
    register("directions", DirectionArgumentType.class, ConstantArgumentSerializer.of(DirectionArgumentType::create));
    register("nbt_predicate", NbtPredicateArgumentType.class, NbtPredicateArgumentType.Serializer.INSTANCE);
    register("nbt_function", NbtFunctionArgumentType.class, NbtFunctionArgumentType.Serializer.INSTANCE);
    register("keyword_args", KeywordArgsArgumentType.class, new KeywordArgsArgumentSerializer());
    register("pos", EnhancedPosArgumentType.class, new EnhancedPosArgumentType.Serializer());
    register("region", RegionArgumentType.class, ConstantArgumentSerializer.of(RegionArgumentType::new));
    RegionTypes.init();
  }

  private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void register(
      String name, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(new Identifier(EnhancedCommands.MOD_ID, name), clazz, serializer);
  }
}
