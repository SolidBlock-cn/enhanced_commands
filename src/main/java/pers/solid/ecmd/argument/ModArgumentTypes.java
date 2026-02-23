package pers.solid.ecmd.argument;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.arguments.ArgumentType;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import pers.solid.ecmd.EnhancedCommands;

/**
 * @see net.minecraft.commands.synchronization.ArgumentTypeInfos
 */
public class ModArgumentTypes {
  public static void init() {
    register("angle", AngleArgumentType.class, AngleArgumentType.Serializer.INSTANCE);
    register("any", AnyTypeArgumentType.class, SingletonArgumentInfo.contextAware(AnyTypeArgumentType::new));
    register("block_predicate", BlockPredicateArgumentType.class, SingletonArgumentInfo.contextAware(BlockPredicateArgumentType::new));
    register("block_function", BlockFunctionArgumentType.class, SingletonArgumentInfo.contextAware(BlockFunctionArgumentType::new));
    register("curve", CurveArgumentType.class, SingletonArgumentInfo.contextAware(CurveArgumentType::new));
    register("direction", DirectionArgumentType.class, SingletonArgumentInfo.contextFree(DirectionArgumentType::direction));
    register("entity_predicate", EntityPredicateArgumentType.class, SingletonArgumentInfo.contextAware(EntityPredicateArgumentType::new));
    register("nbt_function", NbtFunctionArgumentType.class, NbtFunctionArgumentType.Serializer.INSTANCE);
    register("nbt_predicate", NbtPredicateArgumentType.class, NbtPredicateArgumentType.Serializer.INSTANCE);
    register("nbt_source", NbtSourceArgumentType.class, SingletonArgumentInfo.contextAware(NbtSourceArgumentType::new));
    register("nbt_target", NbtTargetArgumentType.class, SingletonArgumentInfo.contextAware(NbtTargetArgumentType::new));
    register("keyword_args", KeywordArgsArgumentType.class, KeywordArgsArgumentSerializer.INSTANCE);
    registerTrustingType("omitted_registry_entry", OmittedRegistryEntryArgumentType.class, OmittedRegistryEntryArgumentType.Serializer.INSTANCE);
    register("pos", EnhancedPosArgumentType.class, EnhancedPosArgumentType.Serializer.INSTANCE);
    register("regex", RegexArgumentType.class, SingletonArgumentInfo.contextFree(Suppliers.ofInstance(RegexArgumentType.REGEX)));
    register("region", RegionArgumentType.class, SingletonArgumentInfo.contextAware(RegionArgumentType::new));
    register("rotation", EnhancedRotationArgumentType.class, SingletonArgumentInfo.contextFree(Suppliers.ofInstance(EnhancedRotationArgumentType.INSTANCE)));
    register("simple_enum", SimpleEnumArgumentType.class, SimpleEnumArgumentType.Serializer.INSTANCE);
    register("string_enum", StringEnumArgumentType.class, StringEnumArgumentType.Serializer.INSTANCE);
    register("unloaded_pos_behavior", UnloadedPosBehaviorArgumentType.class, SingletonArgumentInfo.contextFree(UnloadedPosBehaviorArgumentType::new));
    registerTrustingType("vanilla_wrapped", VanillaWrappedArgumentType.class, VanillaWrappedArgumentType.Serializer.INSTANCE);
  }

  private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void register(
      String name, Class<? extends A> clazz, ArgumentTypeInfo<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(EnhancedCommands.id(name), clazz, serializer);
  }


  @SuppressWarnings("unchecked")
  private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void registerTrustingType(
      // 此方法用于需要使用特殊泛型的方法，和 register 类似，但是为了在特殊情况下编译通过。
      String name, Class<?> clazz, ArgumentTypeInfo<A, T> serializer) {
    ArgumentTypeRegistry.registerArgumentType(EnhancedCommands.id(name), (Class<? extends A>) clazz, serializer);
  }
}
