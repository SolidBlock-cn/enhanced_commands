package pers.solid.ecmd.argument;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.arguments.BoolArgumentType;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.BlockTransformationTask;
import pers.solid.ecmd.util.UnloadedPosBehavior;

import java.util.function.Function;

/**
 * 此命令包含了一些多个命令共用的 {@link KeywordArgsArgumentType} 的部分，从而在序列化时减少冗余。
 */
public final class KeywordArgsCommon {
  private static final Object2ReferenceMap<Identifier, Function<CommandRegistryAccess, KeywordArgsArgumentType>> REGISTRY = new Object2ReferenceOpenHashMap<>();
  private static final Reference2ObjectMap<Function<CommandRegistryAccess, KeywordArgsArgumentType>, Identifier> IDS = new Reference2ObjectOpenHashMap<>();

  private KeywordArgsCommon() {
    assert false;
  }

  public static <T extends Function<CommandRegistryAccess, KeywordArgsArgumentType>> T register(Identifier identifier, T keywordArgsFunction) {
    if (REGISTRY.containsKey(identifier)) {
      throw new IllegalArgumentException("Duplicate shared keyword args id: " + identifier);
    }
    if (IDS.containsKey(keywordArgsFunction)) {
      throw new IllegalArgumentException("Duplicate shared keyword args " + keywordArgsFunction + " with id " + identifier);
    }
    REGISTRY.put(identifier, keywordArgsFunction);
    IDS.put(keywordArgsFunction, identifier);
    return keywordArgsFunction;
  }

  public static Function<CommandRegistryAccess, KeywordArgsArgumentType> register(Identifier identifier, KeywordArgsArgumentType keywordArgs) {
    final Function<CommandRegistryAccess, KeywordArgsArgumentType> constantFunction = ignored -> keywordArgs;
    return register(identifier, constantFunction);
  }

  private static <T extends Function<CommandRegistryAccess, KeywordArgsArgumentType>> T register(String name, T keywordArgsFunction) {
    return register(new Identifier(EnhancedCommands.MOD_ID, name), keywordArgsFunction);
  }

  private static Function<CommandRegistryAccess, KeywordArgsArgumentType> register(String name, KeywordArgsArgumentType keywordArgs) {
    return register(new Identifier(EnhancedCommands.MOD_ID, name), keywordArgs);
  }

  public static @Nullable Identifier getId(Function<CommandRegistryAccess, KeywordArgsArgumentType> target) {
    return IDS.get(target);
  }

  public static @NotNull Identifier getIdOrThrow(Function<CommandRegistryAccess, KeywordArgsArgumentType> target) {
    return Preconditions.checkNotNull(getId(target), "unregistered shared keyword args");
  }

  public static @Nullable Function<CommandRegistryAccess, KeywordArgsArgumentType> getById(Identifier identifier) {
    return REGISTRY.get(identifier);
  }

  public static @NotNull Function<CommandRegistryAccess, KeywordArgsArgumentType> getByIdOrThrow(Identifier identifier) {
    return Preconditions.checkNotNull(getById(identifier), "unknown keyword args type: %s", identifier);
  }

  public static final Function<CommandRegistryAccess, KeywordArgsArgumentType> CONVERT_BLOCKS = register("convert_blocks", KeywordArgsArgumentType.builder()
      .addOptionalArg("skip_light_update", BoolArgumentType.bool(), false)
      .addOptionalArg("notify_listeners", BoolArgumentType.bool(), true)
      .addOptionalArg("notify_neighbors", BoolArgumentType.bool(), false)
      .addOptionalArg("force_state", BoolArgumentType.bool(), true)
      .addOptionalArg("suppress_initial_check", BoolArgumentType.bool(), false)
      .addOptionalArg("suppress_replaced_check", BoolArgumentType.bool(), false)
      .addOptionalArg("force", BoolArgumentType.bool(), false)
      .addOptionalArg("nbt", NbtFunctionArgumentType.COMPOUND, null)
      .addOptionalArg("affect_fluid", BoolArgumentType.bool(), false)
      .build());
  public static final Function<CommandRegistryAccess, KeywordArgsArgumentType> FILLING = register("filling", KeywordArgsArgumentType.builder()
      .addOptionalArg("immediately", BoolArgumentType.bool(), false)
      .addOptionalArg("bypass_limit", BoolArgumentType.bool(), false)
      .addOptionalArg("skip_light_update", BoolArgumentType.bool(), false)
      .addOptionalArg("notify_listeners", BoolArgumentType.bool(), true)
      .addOptionalArg("notify_neighbors", BoolArgumentType.bool(), false)
      .addOptionalArg("force_state", BoolArgumentType.bool(), false)
      .addOptionalArg("post_process", BoolArgumentType.bool(), false)
      .addOptionalArg("unloaded_pos", new UnloadedPosBehaviorArgumentType(), UnloadedPosBehavior.REJECT)
      .addOptionalArg("suppress_initial_check", BoolArgumentType.bool(), false)
      .addOptionalArg("suppress_replaced_check", BoolArgumentType.bool(), false)
      .addOptionalArg("force", BoolArgumentType.bool(), false)
      .build());
  public static final Function<CommandRegistryAccess, KeywordArgsArgumentType> BLOCK_TRANSFORMATION = register("block_transformation", registryAccess -> KeywordArgsArgumentType.builderFromShared(FILLING, registryAccess)
      .addOptionalArg("affect_entities", EntityPredicateArgumentType.entityPredicate(registryAccess), null)
      .addOptionalArg("affect_only", BlockPredicateArgumentType.blockPredicate(registryAccess), null)
      .addOptionalArg("keep_remaining", BoolArgumentType.bool(), false)
      .addOptionalArg("keep_state", BoolArgumentType.bool(), false)
      .addOptionalArg("remaining", BlockFunctionArgumentType.blockFunction(registryAccess), BlockTransformationTask.DEFAULT_REMAINING_FUNCTION)
      .addOptionalArg("select", BoolArgumentType.bool(), false)
      .addOptionalArg("transform_only", BlockPredicateArgumentType.blockPredicate(registryAccess), null).build());
}
