package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.UnloadedPosException;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.iterator.BatchedFilterIterable;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.argument.DirectionArgumentType.direction;
import static pers.solid.ecmd.argument.DirectionArgumentType.getDirection;
import static pers.solid.ecmd.argument.KeywordArgsArgumentType.getKeywordArgs;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum StackCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final SimpleCommandExceptionType UNLOADED_SOURCE = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.commands.stack.rejected_source", "unloaded=" + UnloadedPosBehavior.FORCE.getSerializedName()));
  public static final SimpleCommandExceptionType UNLOADED_TARGET = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.commands.stack.rejected_target", "unloaded=" + UnloadedPosBehavior.FORCE.getSerializedName()));

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    final KeywordArgsArgumentType keywordArgsForVector = KeywordArgsArgumentType.builderFromShared(KeywordArgsCommon.FILLING, registryAccess)
        // 是否一并对实体进行堆叠
        .addOptionalArg("affect_entities", EntityArgument.entities(), null)
        // 是否将活动区域设置为堆叠后的区域
        .addOptionalArg("select", BoolArgumentType.bool(), false)
        // 只堆叠符合指定的谓词的方块
        .addOptionalArg("transform_only", BlockPredicateArgumentType.blockPredicate(registryAccess), null)
        .build();
    final KeywordArgsArgumentType keywordArgsForDirections = KeywordArgsArgumentType.builder().addAll(keywordArgsForVector)
        // 表示不通过检测区域的边界大小来推断偏移值。
        .addOptionalArg("absolute", BoolArgumentType.bool(), false)
        // 每次向该方向堆叠之前的间隔。默认为 0，可以是负数。
        .addOptionalArg("gap", integer(), 0)
        .build();

    ModCommands.registerWithRegionArgumentModification(
        dispatcher,
        literalR2("stack"),
        literalR2("/stack"),
        argument("region", RegionArgumentType.region(registryAccess))
            .then(argument("direction", direction())
                .executes(context -> executeStackInDirection(getDirection(context, "direction"), 1, keywordArgsForDirections.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgsForDirections)
                    .executes(context -> executeStackInDirection(getDirection(context, "direction"), 1, getKeywordArgs(context, "keyword_args"), context))))
            .then(argument("keyword_args", keywordArgsForDirections)
                .executes(context -> executeStackInDirection(DirectionArgument.FRONT.apply(context.getSource()), 1, getKeywordArgs(context, "keyword_args"), context)))
            .then(argument("amount", integer())
                .executes(context -> executeStackInDirection(DirectionArgument.FRONT.apply(context.getSource()), getInteger(context, "amount"), keywordArgsForDirections.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgsForDirections)
                    .executes(context -> executeStackInDirection(DirectionArgument.FRONT.apply(context.getSource()), getInteger(context, "amount"), getKeywordArgs(context, "keyword_args"), context)))
                .then(argument("direction", DirectionArgumentType.direction())
                    .executes(context -> executeStackInDirection(getDirection(context, "direction"), getInteger(context, "amount"), keywordArgsForDirections.defaultArgs(), context))
                    .then(argument("keyword_args", keywordArgsForDirections)
                        .executes(context -> executeStackInDirection(getDirection(context, "direction"), getInteger(context, "amount"), getKeywordArgs(context, "keyword_args"), context))))
                .then(literal("vector")
                    .then(argument("x", integer())
                        .then(argument("y", integer())
                            .then(argument("z", integer())
                                .executes(context -> executeStack(new Vec3i(getInteger(context, "x"), getInteger(context, "y"), getInteger(context, "z")), getInteger(context, "amount"), keywordArgsForVector.defaultArgs(), context))
                                .then(argument("keyword_args", keywordArgsForVector)
                                    .executes(context -> executeStack(new Vec3i(getInteger(context, "x"), getInteger(context, "y"), getInteger(context, "z")), getInteger(context, "amount"), getKeywordArgs(context, "keyword_args"), context))))))))
            .executes(context -> executeStackInDirection(DirectionArgument.FRONT.apply(context.getSource()), 1, keywordArgsForDirections.defaultArgs(), context))
    );
  }

  public static int executeStackInDirection(Direction direction, int stackAmount, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeStackInDirection(RegionArgumentType.getRegion(context, "region"), direction, stackAmount, keywordArgs, context);
  }

  public static final SimpleCommandExceptionType UNSUPPORTED_BOX = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.commands.stack.unsupported_box"));

  public static int executeStackInDirection(Region region, Direction direction, int stackAmount, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final BoundingBox blockBox = region.minContainingBlockBox();
    final int offsetAmount;
    if (blockBox == null) {
      throw UNSUPPORTED_BOX.create();
    } else if (!keywordArgs.getBoolean("absolute")) {
      final Direction.Axis axis = direction.getAxis();
      offsetAmount = axis.choose(blockBox.maxX(), blockBox.maxY(), blockBox.maxZ()) - axis.choose(blockBox.minX(), blockBox.minY(), blockBox.minZ()) + 1 + keywordArgs.getInt("gap");
    } else {
      offsetAmount = keywordArgs.getInt("gap");
    }
    return executeStack(region, direction.getNormal().multiply(offsetAmount), stackAmount, keywordArgs, context);
  }

  public static int executeStack(Vec3i relativeVec, int stackAmount, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeStack(RegionArgumentType.getRegion(context, "region"), relativeVec, stackAmount, keywordArgs, context);
  }

  public static int executeStack(Region region, Vec3i relativeVec, int stackAmount, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final ServerLevel world = source.getLevel();
    final Vec3i multiplied = relativeVec.multiply(stackAmount);
    final Region targetRegion = region.moved(multiplied);
    final boolean transformsRegion = keywordArgs.getBoolean("select");
    final ServerPlayer player = source.getPlayer();

    final UnloadedPosBehavior unloadedPosBehavior = keywordArgs.getArg("unloaded_pos");
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BoundingBox blockBox = region.minContainingBlockBox();
      final BoundingBox blockBox2 = targetRegion.minContainingBlockBox();
      if (blockBox != null && !LoadUtil.isPosLoaded(world, blockBox)) {
        throw UNLOADED_SOURCE.create();
      }
      if (blockBox2 != null && !LoadUtil.isPosLoaded(world, blockBox2)) {
        throw UNLOADED_TARGET.create();
      }
    }
    final @Nullable Long seed = keywordArgs.getArg("seed");
    final ExecutionContext executionContext = new ExecutionContext(world.getRandom(), source, seed);

    final Long2ReferenceMap<BlockState> sourceStates = new Long2ReferenceLinkedOpenHashMap<>();
    final Long2ReferenceMap<CompoundTag> sourceBlockEntities = new Long2ReferenceLinkedOpenHashMap<>();
    final ObjectList<Triple<Vec3, EntityType<?>, CompoundTag>> sourceEntities = new ObjectArrayList<>();
    final MutableBoolean hasUnloadedPos = new MutableBoolean();

    final BlockPredicate affectOnly = keywordArgs.getArg("affect_only");
    final BlockPredicate transformOnly = keywordArgs.getArg("transform_only");


    final MutableComponent taskName = Component.translatable("enhanced_commands.commands.stack.task_name", region.asString(), Integer.toString(stackAmount));
    final int flags = FillReplaceCommand.getFlags(keywordArgs);
    final int modFlags = FillReplaceCommand.getModFlags(keywordArgs);
    final @Nullable BlockPlacementHistory history = keywordArgs.getBoolean("undoable") ? new BlockPlacementHistory(taskName, world, flags, modFlags) : null;

    // 收集需要影响的方块和方块实体
    Iterable<@Nullable BlockPos> posIterable;
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      posIterable = UnloadedPosException.catching(Iterables.transform(region, blockPos -> {
        if (!world.hasChunkAt(blockPos)) {
          hasUnloadedPos.setTrue();
          throw new UnloadedPosException(blockPos.immutable());
        }
        return blockPos;
      }));
    } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      posIterable = new BatchedFilterIterable<>(region, 16, blockPos -> {
        final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) hasUnloadedPos.setTrue();
        return chunkLoaded;
      });
    } else {
      posIterable = region;
    }
    final Iterable<Void> collectSourceBlocks = Iterables.transform(posIterable, blockPos -> {
      if (blockPos == null) return null;
      final BlockInWorld cachedBlockPosition = new BlockInWorld(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
      if (transformOnly == null || transformOnly.test(cachedBlockPosition, executionContext)) {
        sourceStates.put(blockPos.asLong(), cachedBlockPosition.getState());
        if (cachedBlockPosition.getEntity() != null) {
          sourceBlockEntities.put(blockPos.asLong(), cachedBlockPosition.getEntity().saveWithoutMetadata(world.registryAccess()));
        }
      }
      return null;
    });

    // 收集需要影响的实体
    final EntitySelector affectEntities = keywordArgs.getArg("affect_entities");
    final Iterable<Void> collectSourceEntities;
    if (affectEntities != null) {
      final List<? extends @NotNull Entity> entities = affectEntities.findEntities(source).stream().filter(entity -> region.contains(entity.position())).toList();
      collectSourceEntities = Iterables.transform(entities, entity -> {
        sourceEntities.add(new ImmutableTriple<>(entity.position(), entity.getType(), entity.saveWithoutId(new CompoundTag())));
        return null;
      });
    } else {
      collectSourceEntities = Collections.emptyList();
    }

    // 此操作过程影响的方块数量。注意：当 offset 为负数时，一个位置的方块可能被重复多次设置，这种情况下会被记录为多次。
    MutableInt blocksAffected = new MutableInt();
    // 此操作过程复制的实体数量。
    MutableInt entitiesAffected = new MutableInt();

    final BlockPos.MutableBlockPos stackedRelativePos = new BlockPos.MutableBlockPos();
    final BlockPos.MutableBlockPos posToPlace = new BlockPos.MutableBlockPos();

    final Long2ReferenceMap<BlockState> oldStates = new Long2ReferenceLinkedOpenHashMap<>();

    final boolean immediately = keywordArgs.getBoolean("immediately");
    final boolean useTasks = !immediately && region.numberOfBlocksAffected() * stackAmount > 16384;
    final Iterable<Void> executeStack = Iterables.concat((Iterable<Iterable<Void>>) () -> IntStream.rangeClosed(1, stackAmount).mapToObj(stackId -> {
      final Long2LongMap stackedToSourceOnThisStack = new Long2LongArrayMap();
      stackedRelativePos.set(relativeVec.multiply(stackId));

      Iterable<Void> collectPosToAffectOnThickStack = () -> {
        Stream<LongLongPair> posPairStream = sourceStates.keySet().longStream().mapToObj(sourcePosLong -> {
          posToPlace.set(sourcePosLong).move(stackedRelativePos);

          return LongLongPair.of(posToPlace.asLong(), sourcePosLong);
        });

        if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
          posPairStream = posPairStream.takeWhile(pair -> {
            if (!world.hasChunkAt(posToPlace.set(pair.firstLong()))) {
              hasUnloadedPos.setTrue();
              return false;
            }
            return true;
          });
        }
        if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
          posPairStream = posPairStream.filter(pair -> {
            final boolean chunkLoaded = world.hasChunkAt(posToPlace.set(pair.firstLong()));
            if (!chunkLoaded) hasUnloadedPos.setTrue();
            return chunkLoaded;
          });
        }

        return posPairStream.map(pair -> {
          posToPlace.set(pair.firstLong());

          final BlockInWorld cachedBlockPosition = new BlockInWorld(world, posToPlace, false);
          if (affectOnly == null || affectOnly.test(cachedBlockPosition, executionContext)) {
            oldStates.put(posToPlace.asLong(), cachedBlockPosition.getState());
            stackedToSourceOnThisStack.put(posToPlace.asLong(), pair.secondLong());
          }
          return (Void) null;
        }).iterator();
      };
      if (useTasks) {
        collectPosToAffectOnThickStack = IterateUtils.batchAndSkip(collectPosToAffectOnThickStack, 16384, 1);
      }

      Iterable<Void> setBlocksOnThisStack = Iterables.transform(stackedToSourceOnThisStack.long2LongEntrySet(), entry -> {
        final BlockState newState = sourceStates.get(entry.getLongValue());
        final BlockEntity oldEntity = world.getBlockEntity(posToPlace.set(entry.getLongKey()));
        if (history != null) {
          history.recordBlockAndEntity(world, posToPlace, oldStates.get(entry.getLongKey()), newState);
        }
        if (oldEntity != null && !oldEntity.isValidBlockState(newState)) {
          world.removeBlockEntity(posToPlace);
        }

        boolean modified = MixinShared.setBlockStateWithModFlags(world, posToPlace, newState, flags, modFlags);

        final BlockEntity newEntity = world.getBlockEntity(posToPlace);
        if (newEntity != null) {
          final CompoundTag nbtCompound = sourceBlockEntities.get(entry.getLongValue());
          if (nbtCompound != null) {
            newEntity.loadWithComponents(nbtCompound, world.registryAccess());
            modified = true;
          }
        }
        if (modified) blocksAffected.increment();
        return null;
      });
      setBlocksOnThisStack = UnloadedPosException.catching(setBlocksOnThisStack);
      if (useTasks) {
        setBlocksOnThisStack = IterateUtils.batchAndSkip(setBlocksOnThisStack, 16384, 7);
      }

      Iterable<Void> stackEntitiesOnThisStack = Iterables.transform(sourceEntities, triple -> {
        final Vec3 vec3d = triple.getLeft().add(stackedRelativePos.getX(), stackedRelativePos.getY(), stackedRelativePos.getZ());
        final EntityType<?> entityType = triple.getMiddle();
        final CompoundTag nbt = triple.getRight();

        final Entity newEntity = entityType.create(world);
        if (newEntity != null) {
          newEntity.load(nbt);
          newEntity.setPos(vec3d);
          newEntity.setUUID(Mth.createInsecureUUID(world.getRandom()));
          world.addFreshEntity(newEntity);
          entitiesAffected.increment();
        }
        return null;
      });
      if (useTasks) {
        stackEntitiesOnThisStack = IterateUtils.batchAndSkip(stackEntitiesOnThisStack, 32767, 15);
      }

      return Iterables.concat(collectPosToAffectOnThickStack, setBlocksOnThisStack, stackEntitiesOnThisStack);
    }).iterator());

    final Iterable<Void> finalClaim = IterateUtils.singletonPeekingIterable(() -> {
      if (hasUnloadedPos.booleanValue()) {
        if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.broken").withStyle(Styles.ACTUAL), false);
        } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.skipped").withStyle(Styles.ACTUAL), false);
        }
      }
      if (affectEntities != null) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.stack.complete_including_entities", blocksAffected, entitiesAffected), true);
      } else {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.stack.complete", blocksAffected), true);
      }
      if (transformsRegion && player != null) {
        final RegionSelection activeRegion = player.getActiveRegion$ec();
        if (activeRegion != null && region.equals(activeRegion.region())) {
          try {
            player.setActiveRegion$ec(activeRegion.moved(multiplied));
          } catch (CommandSyntaxException e) {
            throw new CommandRuntimeException(e);
          }
        }
      }
    });

    if (history != null) {
      final HistoryHolder historyHolder = HistoryHolder.fromSource(source);
      if (historyHolder != null) {
        historyHolder.addUndoableHistory$ec(history);
      }
    }
    if (useTasks) {
      // The region is too large. Send a server task.
      final IteratorTask<?> task = ((ThreadExecutorExtension) source.getServer()).addIteratorTask$ec(taskName, Iterables.concat(
          IterateUtils.batchAndSkip(collectSourceBlocks, 16384, 3),
          IterateUtils.batchAndSkip(collectSourceEntities, 16384, 3),
          executeStack,
          finalClaim
      ).iterator());
      if (history != null) {
        history.task = task;
      }
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.large_region", Long.toString(region.numberOfBlocksAffected())).withStyle(ChatFormatting.YELLOW), true);
      return 1;
    } else {
      try {
        IterateUtils.exhaust(Iterables.concat(collectSourceBlocks, collectSourceEntities, executeStack, finalClaim).iterator());
      } catch (CommandRuntimeException e) {
        if (e.getCause() instanceof CommandSyntaxException es) {
          throw es;
        } else {
          throw e;
        }
      }
      return blocksAffected.intValue() + entitiesAffected.intValue();
    }
  }
}
