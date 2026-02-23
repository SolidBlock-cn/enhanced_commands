package pers.solid.ecmd.argument;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.block.BlockTransformationTask;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;

import java.util.function.Function;

/**
 * 此命令包含了一些多个命令共用的 {@link KeywordArgsArgumentType} 的部分，从而在序列化时减少冗余。
 */
public final class KeywordArgsCommon {
  private static final Object2ReferenceMap<ResourceLocation, Function<CommandBuildContext, KeywordArgsArgumentType>> REGISTRY = new Object2ReferenceOpenHashMap<>();
  private static final Reference2ObjectMap<Function<CommandBuildContext, KeywordArgsArgumentType>, ResourceLocation> IDS = new Reference2ObjectOpenHashMap<>();

  private KeywordArgsCommon() {
    assert false;
  }

  public static <T extends Function<CommandBuildContext, KeywordArgsArgumentType>> T register(ResourceLocation identifier, T keywordArgsFunction) {
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

  public static Function<CommandBuildContext, KeywordArgsArgumentType> register(ResourceLocation identifier, KeywordArgsArgumentType keywordArgs) {
    final Function<CommandBuildContext, KeywordArgsArgumentType> constantFunction = ignored -> keywordArgs;
    return register(identifier, constantFunction);
  }

  private static <T extends Function<CommandBuildContext, KeywordArgsArgumentType>> T register(String name, T keywordArgsFunction) {
    return register(EnhancedCommands.id(name), keywordArgsFunction);
  }

  private static Function<CommandBuildContext, KeywordArgsArgumentType> register(String name, KeywordArgsArgumentType keywordArgs) {
    return register(EnhancedCommands.id(name), keywordArgs);
  }

  public static @Nullable ResourceLocation getId(Function<CommandBuildContext, KeywordArgsArgumentType> target) {
    return IDS.get(target);
  }

  public static @NotNull ResourceLocation getIdOrThrow(Function<CommandBuildContext, KeywordArgsArgumentType> target) {
    return Preconditions.checkNotNull(getId(target), "unregistered shared keyword args");
  }

  public static @Nullable Function<CommandBuildContext, KeywordArgsArgumentType> getById(ResourceLocation identifier) {
    return REGISTRY.get(identifier);
  }

  public static @NotNull Function<CommandBuildContext, KeywordArgsArgumentType> getByIdOrThrow(ResourceLocation identifier) {
    return Preconditions.checkNotNull(getById(identifier), "unknown keyword args type: %s", identifier);
  }

  public static final Function<CommandBuildContext, KeywordArgsArgumentType> CONVERT_BLOCKS = register("convert_blocks", commandBuildContext -> KeywordArgsArgumentType.builder()
      .addOptionalArg("skip_light_update", BoolArgumentType.bool(), false)
      .addOptionalArg("notify_listeners", BoolArgumentType.bool(), true)
      .addOptionalArg("notify_neighbors", BoolArgumentType.bool(), false)
      .addOptionalArg("force_state", BoolArgumentType.bool(), true)
      .addOptionalArg("suppress_initial_check", BoolArgumentType.bool(), false)
      .addOptionalArg("suppress_replaced_check", BoolArgumentType.bool(), false)
      .addOptionalArg("force", BoolArgumentType.bool(), false)
      .addOptionalArg("nbt", NbtFunctionArgumentType.compound(commandBuildContext), null)
      .addOptionalArg("affect_fluid", BoolArgumentType.bool(), false)
      .addOptionalArg("seed", LongArgumentType.longArg(), null)
      .build());
  public static final Function<CommandBuildContext, KeywordArgsArgumentType> FILLING = register("filling", commandBuildContext -> KeywordArgsArgumentType.builder()
      .addOptionalArg("affect_only", BlockPredicateArgumentType.blockPredicate(commandBuildContext), null)
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
      .addOptionalArg("undoable", BoolArgumentType.bool(), true)
      .addOptionalArg("seed", LongArgumentType.longArg(), null)
      .build());
  public static final Function<CommandBuildContext, KeywordArgsArgumentType> BLOCK_TRANSFORMATION = register("block_transformation", commandBuildContext -> KeywordArgsArgumentType.builderFromShared(FILLING, commandBuildContext)
      .addOptionalArg("affect_entities", EntityPredicateArgumentType.entityPredicate(commandBuildContext), null)
      .addOptionalArg("keep_remaining", BoolArgumentType.bool(), false)
      .addOptionalArg("keep_state", BoolArgumentType.bool(), false)
      .addOptionalArg("remaining", BlockFunctionArgumentType.blockFunction(commandBuildContext), BlockTransformationTask.DEFAULT_REMAINING_FUNCTION)
      .addOptionalArg("select", BoolArgumentType.bool(), false)
      .addOptionalArg("transform_only", BlockPredicateArgumentType.blockPredicate(commandBuildContext), null).build());
}
