package pers.solid.ecmd.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import pers.solid.ecmd.EnhancedCommands;

/**
 * @see net.minecraft.command.argument.ArgumentTypes
 */
public class ModArgumentTypes {
  public static void init() {
    register("angle", AngleArgumentType.class, AngleArgumentType.Serializer.INSTANCE);
    register("block_predicate", BlockPredicateArgumentType.class, ConstantArgumentSerializer.of(BlockPredicateArgumentType::new));
    register("block_function", BlockFunctionArgumentType.class, ConstantArgumentSerializer.of(BlockFunctionArgumentType::new));
    register("curve", CurveArgumentType.class, ConstantArgumentSerializer.of(CurveArgumentType::new));
    register("direction", DirectionArgumentType.class, ConstantArgumentSerializer.of(DirectionArgumentType::direction));
    register("entity_predicate", EntityPredicateArgumentType.class, ConstantArgumentSerializer.of(EntityPredicateArgumentType::new));
    register("nbt_function", NbtFunctionArgumentType.class, NbtFunctionArgumentType.Serializer.INSTANCE);
    register("nbt_predicate", NbtPredicateArgumentType.class, NbtPredicateArgumentType.Serializer.INSTANCE);
    register("nbt_source", NbtSourceArgumentType.class, ConstantArgumentSerializer.of(NbtSourceArgumentType::new));
    register("nbt_target", NbtTargetArgumentType.class, ConstantArgumentSerializer.of(NbtTargetArgumentType::new));
    register("keyword_args", KeywordArgsArgumentType.class, KeywordArgsArgumentSerializer.INSTANCE);
    registerTrustingType("omitted_registry_entry", OmittedRegistryEntryArgumentType.class, OmittedRegistryEntryArgumentType.Serializer.INSTANCE);
    register("pos", EnhancedPosArgumentType.class, EnhancedPosArgumentType.Serializer.INSTANCE);
    register("regex", RegexArgumentType.class, ConstantArgumentSerializer.of(() -> RegexArgumentType.REGEX));
    register("region", RegionArgumentType.class, ConstantArgumentSerializer.of(RegionArgumentType::new));
    register("simple_enum", SimpleEnumArgumentType.class, SimpleEnumArgumentType.Serializer.INSTANCE);
    register("string_enum", StringEnumArgumentType.class, StringEnumArgumentType.Serializer.INSTANCE);
    register("test_type", TestTypeArgumentType.class, ConstantArgumentSerializer.of(TestTypeArgumentType::new));
    register("unloaded_pos_behavior", UnloadedPosBehaviorArgumentType.class, ConstantArgumentSerializer.of(UnloadedPosBehaviorArgumentType::new));
    registerTrustingType("vanilla_wrapped", VanillaWrappedArgumentType.class, VanillaWrappedArgumentType.Serializer.INSTANCE);
  }

  private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void register(
      String name, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(EnhancedCommands.id(name), clazz, serializer);
  }


  @SuppressWarnings("unchecked")
  private static <A extends ArgumentType<?>, T extends ArgumentSerializer.ArgumentTypeProperties<A>> void registerTrustingType(
      // 此方法用于需要使用特殊泛型的方法，和 register 类似，但是为了在特殊情况下编译通过。
      String name, Class<?> clazz, ArgumentSerializer<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(EnhancedCommands.id(name), (Class<? extends A>) clazz, serializer);
  }
}
