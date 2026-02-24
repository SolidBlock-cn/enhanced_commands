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
    register("angle", AngleArgument.class, AngleArgument.Info.INSTANCE);
    register("any", AnyTypeArgument.class, SingletonArgumentInfo.contextAware(AnyTypeArgument::new));
    register("block_predicate", BlockPredicateArgument.class, SingletonArgumentInfo.contextAware(BlockPredicateArgument::new));
    register("block_function", BlockFunctionArgument.class, SingletonArgumentInfo.contextAware(BlockFunctionArgument::new));
    register("curve", CurveArgument.class, SingletonArgumentInfo.contextAware(CurveArgument::new));
    register("direction", DirectionArgument.class, SingletonArgumentInfo.contextFree(DirectionArgument::direction));
    register("entity_predicate", EntityPredicateArgument.class, SingletonArgumentInfo.contextAware(EntityPredicateArgument::new));
    register("nbt_function", NbtFunctionArgument.class, NbtFunctionArgument.Info.INSTANCE);
    register("nbt_predicate", NbtPredicateArgument.class, NbtPredicateArgument.Info.INSTANCE);
    register("nbt_source", NbtSourceArgument.class, SingletonArgumentInfo.contextAware(NbtSourceArgument::new));
    register("nbt_target", NbtTargetArgument.class, SingletonArgumentInfo.contextAware(NbtTargetArgument::new));
    register("keyword_args", KeywordArgsArgument.class, KeywordArgsArgumentTypeInfo.INSTANCE);
    registerTrustingType("omitted_registry_entry", OmittedRegistryEntryArgument.class, OmittedRegistryEntryArgument.Info.INSTANCE);
    register("pos", EnhancedPosArgument.class, EnhancedPosArgument.Info.INSTANCE);
    register("regex", RegexArgument.class, SingletonArgumentInfo.contextFree(Suppliers.ofInstance(RegexArgument.REGEX)));
    register("region", RegionArgument.class, SingletonArgumentInfo.contextAware(RegionArgument::new));
    register("rotation", EnhancedRotationArgument.class, SingletonArgumentInfo.contextFree(Suppliers.ofInstance(EnhancedRotationArgument.INSTANCE)));
    registerTrustingType("simple_enum", SimpleEnumArgument.class, SimpleEnumArgument.Info.INSTANCE);
    register("string_enum", StringEnumArgument.class, StringEnumArgument.Info.INSTANCE);
    register("unloaded_pos_behavior", UnloadedPosBehaviorArgument.class, SingletonArgumentInfo.contextFree(UnloadedPosBehaviorArgument::new));
    registerTrustingType("vanilla_wrapped", VanillaWrappedArgument.class, VanillaWrappedArgument.Info.INSTANCE);
  }

  private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void register(
      String name, Class<? extends A> clazz, ArgumentTypeInfo<A, T> info) {
    ArgumentTypeRegistry.registerArgumentType(EnhancedCommands.id(name), clazz, info);
  }


  @SuppressWarnings("unchecked")
  private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void registerTrustingType(
      // 此方法用于需要使用特殊泛型的方法，和 register 类似，但是为了在特殊情况下编译通过。
      String name, Class<?> clazz, ArgumentTypeInfo<A, T> info) {
    ArgumentTypeRegistry.registerArgumentType(EnhancedCommands.id(name), (Class<? extends A>) clazz, info);
  }
}
