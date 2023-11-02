package pers.solid.ecmd.command;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.UnloadedPosException;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.UnloadedPosBehavior;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.DirectionArgumentType.direction;
import static pers.solid.ecmd.argument.DirectionArgumentType.getDirection;
import static pers.solid.ecmd.argument.KeywordArgsArgumentType.getKeywordArgs;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum StackCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final SimpleCommandExceptionType UNLOADED_SOURCE = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.commands.stack.rejected_source", "unloaded=" + UnloadedPosBehavior.FORCE.asString()));
  public static final SimpleCommandExceptionType UNLOADED_TARGET = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.commands.stack.rejected_target", "unloaded=" + UnloadedPosBehavior.FORCE.asString()));

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgsForVector = KeywordArgsArgumentType.builder(FillReplaceCommand.KEYWORD_ARGS)
        // 是否一并对实体进行堆叠
        .addOptionalArg("affect_entities", EntityArgumentType.entities(), null)
        // 仅允许对符合此谓词的方块进行修改
        .addOptionalArg("affect_only", BlockPredicateArgumentType.blockPredicate(registryAccess), null)
        // 是否将活动区域设置为堆叠后的区域
        .addOptionalArg("select", BoolArgumentType.bool(), false)
        // 只堆叠符合指定的谓词的方块
        .addOptionalArg("transform_only", BlockPredicateArgumentType.blockPredicate(registryAccess), null)
        .build();
    final KeywordArgsArgumentType keywordArgsForDirections = KeywordArgsArgumentType.builder(keywordArgsForVector)
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

  public static int executeStackInDirection(Direction direction, int stackAmount, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeStackInDirection(RegionArgumentType.getRegion(context, "region"), direction, stackAmount, keywordArgs, context);
  }

  public static int executeStackInDirection(Region region, Direction direction, int stackAmount, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final BlockBox blockBox = region.minContainingBlockBox();
    final int offsetAmount;
    if (blockBox == null) {
      throw new CommandException(Text.translatable("enhanced_commands.commands.stack.unsupported_box"));
    } else {
      final Direction.Axis axis = direction.getAxis();
      offsetAmount = axis.choose(blockBox.getMaxX(), blockBox.getMaxY(), blockBox.getMaxZ()) - axis.choose(blockBox.getMinX(), blockBox.getMinY(), blockBox.getMinZ()) + 1 + keywordArgs.getInt("gap");
    }
    return executeStack(region, direction.getVector().multiply(offsetAmount), stackAmount, keywordArgs, context);
  }

  public static int executeStack(Vec3i relativeVec, int stackAmount, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeStack(RegionArgumentType.getRegion(context, "region"), relativeVec, stackAmount, keywordArgs, context);
  }

  public static int executeStack(Region region, Vec3i relativeVec, int stackAmount, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    final Region targetRegion = region.moved(relativeVec.multiply(stackAmount));
    final boolean transformsRegion = keywordArgs.getBoolean("select");
    final ServerPlayerEntity player = source.getPlayer();

    Region activeRegion;
    if (transformsRegion && player != null) {
      ((ServerPlayerEntityExtension) player).ec$setActiveRegion(targetRegion);
      activeRegion = targetRegion;
    } else {activeRegion = null;}
    final UnloadedPosBehavior unloadedPosBehavior = keywordArgs.getArg("unloaded_pos");
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      final BlockBox blockBox = region.minContainingBlockBox();
      final BlockBox blockBox2 = targetRegion.minContainingBlockBox();
      if (blockBox != null && !LoadUtil.isPosLoaded(world, blockBox)) {
        throw UNLOADED_SOURCE.create();
      }
      if (blockBox2 != null && !LoadUtil.isPosLoaded(world, blockBox2)) {
        throw UNLOADED_TARGET.create();
      }
    }

    final Long2ReferenceMap<BlockState> sourceStates = new Long2ReferenceLinkedOpenHashMap<>();
    final Long2ReferenceMap<NbtCompound> sourceBlockEntities = new Long2ReferenceLinkedOpenHashMap<>();
    final ObjectList<Triple<Vec3d, EntityType<?>, NbtCompound>> sourceEntities = new ObjectArrayList<>();
    final MutableBoolean hasUnloadedPos = new MutableBoolean();

    final List<Iterator<?>> iterators = new ArrayList<>();

    final BlockPredicate affectOnly, transformOnly;
    {
      final BlockPredicateArgument affectOnlyArgument = keywordArgs.getArg("affect_only");
      affectOnly = affectOnlyArgument == null ? null : affectOnlyArgument.apply(source);
      final BlockPredicateArgument transformOnlyArgument = keywordArgs.getArg("transform_only");
      transformOnly = transformOnlyArgument == null ? null : transformOnlyArgument.apply(source);
    }

    // 收集需要影响的方块和方块实体
    Stream<BlockPos> stream = region.stream();
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      stream = stream.peek(blockPos -> {
        if (!world.isChunkLoaded(blockPos)) {
          hasUnloadedPos.setTrue();
          throw new UnloadedPosException(blockPos.toImmutable());
        }
      });
    }
    if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      stream = stream.filter(blockPos -> {
        final boolean chunkLoaded = world.isChunkLoaded(blockPos);
        if (!chunkLoaded) hasUnloadedPos.setTrue();
        return chunkLoaded;
      });
    }
    final Stream<Void> collectBlocks = stream
        .map(blockPos -> {
          final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
          if (transformOnly == null || transformOnly.test(cachedBlockPosition)) {
            sourceStates.put(blockPos.asLong(), cachedBlockPosition.getBlockState());
            if (cachedBlockPosition.getBlockEntity() != null) {
              sourceBlockEntities.put(blockPos.asLong(), cachedBlockPosition.getBlockEntity().createNbt());
            }
          }
          return null;
        });
    iterators.add(IterateUtils.batchAndSkip(collectBlocks.iterator(), 16384, 3));

    // 收集需要影响的实体
    final EntitySelector affectEntities = keywordArgs.getArg("affect_entities");
    if (affectEntities != null) {
      final List<? extends Entity> entities = affectEntities.getEntities(source).stream().filter(entity -> region.contains(entity.getPos())).toList();
      final Stream<Void> collectEntities = entities.stream().map(entity -> {
        sourceEntities.add(new ImmutableTriple<>(entity.getPos(), entity.getType(), entity.writeNbt(new NbtCompound())));
        return null;
      });
      iterators.add(IterateUtils.batchAndSkip(collectEntities.iterator(), 16384, 3));
    }

    // 此操作过程影响的方块数量。注意：当 offset 为负数时，一个位置的方块可能被重复多次设置，这种情况下会被记录为多次。。
    MutableInt blocksAffected = new MutableInt();
    // 此操作过程复制的实体数量。
    MutableInt entitiesAffected = new MutableInt();

    final BlockPos.Mutable stackedRelativePos = new BlockPos.Mutable();
    final BlockPos.Mutable posToPlace = new BlockPos.Mutable();
    final Stream<Void> setBlocks = IntStream.rangeClosed(1, stackAmount).mapToObj(i -> {
      stackedRelativePos.set(relativeVec.multiply(i));
      Stream<Long2ReferenceMap.Entry<BlockState>> targetPosStream = sourceStates.long2ReferenceEntrySet().stream()
          .peek(entry -> posToPlace.set(entry.getLongKey()).move(stackedRelativePos));
      if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
        targetPosStream = targetPosStream.peek(entry -> {
          if (!world.isChunkLoaded(posToPlace)) {
            hasUnloadedPos.setTrue();
            throw new UnloadedPosException(posToPlace.toImmutable());
          }
        });
      }
      if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
        targetPosStream = targetPosStream.filter(entry -> {
          final boolean chunkLoaded = world.isChunkLoaded(posToPlace);
          if (!chunkLoaded) hasUnloadedPos.setTrue();
          return chunkLoaded;
        });
      }
      final Stream<Void> affectBlocksStream = targetPosStream
          .map(entry -> {
            if (affectOnly == null || affectOnly.test(new CachedBlockPosition(world, posToPlace, false))) {
              boolean modofied = MixinSharedVariables.setBlockStateWithModFlags(world, posToPlace, entry.getValue(), FillReplaceCommand.getFlags(keywordArgs), FillReplaceCommand.getModFlags(keywordArgs));

              final BlockEntity blockEntity = world.getBlockEntity(posToPlace);
              if (blockEntity != null) {
                final NbtCompound nbtCompound = sourceBlockEntities.get(posToPlace.asLong());
                if (nbtCompound != null) {
                  blockEntity.readNbt(nbtCompound);
                  modofied = true;
                }
              }
              if (modofied) blocksAffected.increment();
            }
            return null;
          });
      if (affectEntities == null) return affectBlocksStream;
      final Stream<Void> affectEntitiesStream = sourceEntities.stream().map(triple -> {
        final Vec3d vec3d = triple.getLeft().add(stackedRelativePos.getX(), stackedRelativePos.getY(), stackedRelativePos.getZ());
        final EntityType<?> entityType = triple.getMiddle();
        final NbtCompound nbt = triple.getRight();

        final Entity newEntity = entityType.create(world);
        if (newEntity != null) {
          newEntity.readNbt(nbt);
          newEntity.setPosition(vec3d);
          newEntity.setUuid(MathHelper.randomUuid(world.getRandom()));
          world.spawnEntity(newEntity);
          entitiesAffected.increment();
        }
        return null;
      });
      return Stream.concat(affectBlocksStream, affectEntitiesStream);
    }).flatMap(Function.identity());
    iterators.add(IterateUtils.batchAndSkip(setBlocks.iterator(), 32767, 15));

    Iterator<?> iterator = Iterators.concat(UnloadedPosException.catching(Iterators.concat(iterators.iterator())), IterateUtils.singletonPeekingIterator(() -> {
      if (hasUnloadedPos.booleanValue()) {
        if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
          CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.fill.broken").styled(TextUtil.STYLE_FOR_ACTUAL), false);
        } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
          CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.fill.skipped").styled(TextUtil.STYLE_FOR_ACTUAL), false);
        }
      }
      if (affectEntities != null) {
        CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.stack.complete_including_entities", blocksAffected, entitiesAffected), true);
      } else {
        CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.stack.complete", blocksAffected), true);
      }
      if (transformsRegion && player != null) {
        if (activeRegion != null) {
          ((ServerPlayerEntityExtension) player).ec$setActiveRegion(activeRegion);
        }
      }
    }));

    final boolean immediately = keywordArgs.getBoolean("immediately");
    if (!immediately && region.numberOfBlocksAffected() * stackAmount > 16384) {
      // The region is too large. Send a server task.
      ((ThreadExecutorExtension) source.getServer()).ec_addIteratorTask(Text.translatable("enhanced_commands.commands.stack.task_name", region.asString(), Integer.toString(stackAmount)), UnloadedPosException.catching(iterator));
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.fill.large_region", Long.toString(region.numberOfBlocksAffected())).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(iterator);
      return blocksAffected.intValue() + entitiesAffected.intValue();
    }
  }
}
