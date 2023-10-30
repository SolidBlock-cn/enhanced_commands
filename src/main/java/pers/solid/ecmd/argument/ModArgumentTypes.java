package pers.solid.ecmd.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import pers.solid.ecmd.EnhancedCommands;

/**
 * @see net.minecraft.command.argument.ArgumentTypes
 */
public class ModArgumentTypes {
  public static void init() {
    register("angle", AngleArgumentType.class, new AngleArgumentType.Serializer());
    register("axis", AxisArgumentType.class, new AxisArgumentType.Serializer());
    register("block_predicate", BlockPredicateArgumentType.class, ConstantArgumentSerializer.of(BlockPredicateArgumentType::new));
    register("block_function", BlockFunctionArgumentType.class, ConstantArgumentSerializer.of(BlockFunctionArgumentType::new));
    register("curve", CurveArgumentType.class, ConstantArgumentSerializer.of(CurveArgumentType::new));
    register("direction", DirectionArgumentType.class, ConstantArgumentSerializer.of(DirectionArgumentType::direction));
    register("direction_type", SimpleEnumArgumentTypes.DirectionTypeArgumentType.class, ConstantArgumentSerializer.of(SimpleEnumArgumentTypes.DirectionTypeArgumentType::new));
    register("nbt_predicate", NbtPredicateArgumentType.class, NbtPredicateArgumentType.Serializer.INSTANCE);
    register("nbt_function", NbtFunctionArgumentType.class, NbtFunctionArgumentType.Serializer.INSTANCE);
    register("keyword_args", KeywordArgsArgumentType.class, new KeywordArgsArgumentSerializer());
    register("omitted_registry_entry", OmittedRegistryEntryArgumentType.class, new OmittedRegistryEntryArgumentType.Serializer());
    register("outline_type", SimpleEnumArgumentTypes.OutlineTypeArgumentType.class, ConstantArgumentSerializer.of(SimpleEnumArgumentTypes.OutlineTypeArgumentType::new));
    register("pos", EnhancedPosArgumentType.class, new EnhancedPosArgumentType.Serializer());
    register("region", RegionArgumentType.class, ConstantArgumentSerializer.of(RegionArgumentType::new));
    register("unloaded_pos_behavior", UnloadedPosBehaviorArgumentType.class, ConstantArgumentSerializer.of(UnloadedPosBehaviorArgumentType::new));
    register("vanilla_wrapped", VanillaWrappedArgumentType.class, VanillaWrappedArgumentType.Serializer.INSTANCE);
  }

  private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void register(
      String name, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(new Identifier(EnhancedCommands.MOD_ID, name), clazz, serializer);
  }
}
