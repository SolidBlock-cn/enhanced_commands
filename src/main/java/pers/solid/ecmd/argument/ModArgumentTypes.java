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
    register("angle", AngleArgumentType.class, AngleArgumentType.Serializer.INSTANCE);
    register("axis", AxisArgumentType.class, AxisArgumentType.Serializer.INSTANCE);
    register("block_predicate", BlockPredicateArgumentType.class, ConstantArgumentSerializer.of(BlockPredicateArgumentType::new));
    register("block_function", BlockFunctionArgumentType.class, ConstantArgumentSerializer.of(BlockFunctionArgumentType::new));
    register("concentration_type", ConcentrationTypeArgumentType.class, ConstantArgumentSerializer.of(ConcentrationTypeArgumentType::new));
    register("curve", CurveArgumentType.class, ConstantArgumentSerializer.of(CurveArgumentType::new));
    register("direction", DirectionArgumentType.class, ConstantArgumentSerializer.of(DirectionArgumentType::direction));
    register("direction_type", SimpleEnumArgumentTypes.DirectionTypeArgumentType.class, ConstantArgumentSerializer.of(SimpleEnumArgumentTypes.DirectionTypeArgumentType::new));
    register("entity_predicate", EntityPredicateArgumentType.class, ConstantArgumentSerializer.of(EntityPredicateArgumentType::new));
    register("nbt_function", NbtFunctionArgumentType.class, NbtFunctionArgumentType.Serializer.INSTANCE);
    register("nbt_predicate", NbtPredicateArgumentType.class, NbtPredicateArgumentType.Serializer.INSTANCE);
    register("nbt_source", NbtSourceArgumentType.class, ConstantArgumentSerializer.of(NbtSourceArgumentType::new));
    register("nbt_target", NbtTargetArgumentType.class, ConstantArgumentSerializer.of(NbtTargetArgumentType::new));
    register("keyword_args", KeywordArgsArgumentType.class, KeywordArgsArgumentSerializer.INSTANCE);
    registerTrustingType("omitted_registry_entry", OmittedRegistryEntryArgumentType.class, OmittedRegistryEntryArgumentType.Serializer.INSTANCE);
    register("outline_type", SimpleEnumArgumentTypes.OutlineTypeArgumentType.class, ConstantArgumentSerializer.of(SimpleEnumArgumentTypes.OutlineTypeArgumentType::new));
    register("pos", EnhancedPosArgumentType.class, EnhancedPosArgumentType.Serializer.INSTANCE);
    register("region", RegionArgumentType.class, ConstantArgumentSerializer.of(RegionArgumentType::new));
    register("unloaded_pos_behavior", UnloadedPosBehaviorArgumentType.class, ConstantArgumentSerializer.of(UnloadedPosBehaviorArgumentType::new));
    registerTrustingType("vanilla_wrapped", VanillaWrappedArgumentType.class, VanillaWrappedArgumentType.Serializer.INSTANCE);
  }

  private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void register(
      String name, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(new Identifier(EnhancedCommands.MOD_ID, name), clazz, serializer);
  }


  @SuppressWarnings("unchecked")
  private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void registerTrustingType(
      // 此方法用于需要使用特殊泛型的方法，和 register 类似，但是为了在特殊情况下编译通过。
      String name, Class<?> clazz, ArgumentSerializer<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(new Identifier(EnhancedCommands.MOD_ID, name), (Class<? extends A>) clazz, serializer);
  }
}
