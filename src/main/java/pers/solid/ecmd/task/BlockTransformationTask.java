package pers.solid.ecmd.task;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.block.UnloadedPosException;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.BlockFunctionContext;
import pers.solid.ecmd.block.function.SimpleBlockFunction;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.command.SetReplaceBlocksCommand;
import pers.solid.ecmd.config.BlockOperationConfig;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.history.BlockTransformationHistory;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.extension.HistoryHolder;
import pers.solid.ecmd.util.iterator.AbstractIteratorTask;
import pers.solid.ecmd.util.iterator.BatchedFilterIterable;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 涉及对区域内的方块（可能含有实体）进行变换的任务。此类能够处理一系列复杂的变换过程。
 */
public class BlockTransformationTask extends AbstractIteratorTask {
  public static final Logger LOGGER = LoggerFactory.getLogger(BlockTransformationTask.class);
  /**
   * 对变换后的原有的方块的处理方式，默认为直接使用空气填充。
   */
  public static final BlockFunction DEFAULT_REMAINING_FUNCTION = new SimpleBlockFunction(Blocks.AIR, Collections.emptyList());
  public final boolean shouldTransformRegion;
  public final Consumer<BlockTransformationTask> completionNotifier;
  /**
   * 转换方块坐标的函数。每个方块的坐标都将根据此函数转换至新的坐标。
   */
  private final Function<Vec3i, Vec3i> blockPosTransformer;
  /**
   * 转换坐标的函数。涉及到的实体将根据此函数转换至新的坐标。
   */
  private final @Nullable Function<Vec3, Vec3> posTransformer;
  /**
   * 反向转换坐标的函数，主要用于插值。
   */
  private final @Nullable Function<Vec3, Vec3> invertedPosTransformer;
  /**
   * 转换方块状态的函数，例如有些情况下需要将方块镜像或旋转。
   */
  private final Function<BlockState, BlockState> blockStateTransformer;
  /**
   * 转换实体的 Consumer，不会另行返回值，例如有些情况下需要修改实体的水平朝向。
   */
  private final @Nullable Consumer<Entity> entityTransformer;
  private final @Nullable Consumer<Entity> reverseEntityTransformer;
  private final Level world;
  /**
   * 受操作影响的区域。
   */
  private final Region region;
  private final ExecutionContext executionContext;
  private final BlockFunctionContext blockFunctionContext;
  /**
   * 如果不为 {@code null}，那么只有符合此谓词的方块才会受到影响，包括替换新位置的方块以及按照规则替换原位置的方块。
   */
  private final @Nullable BlockPredicate affectsOnly;
  /**
   * 如果不为 {@code null}，那么只有符合此谓词的方块才会被转换，其他方块则被忽略，不会移动至新位置
   */
  private final @Nullable BlockPredicate transformsOnly;
  /**
   * 如果为不 {@code null}，则方块转换到的旧的位置的方块按照此函数处理。
   */
  private final @Nullable BlockFunction remaining;
  private final @Nullable Iterator<? extends Entity> entitiesToAffect;
  /**
   * 转换过程中是否进行插值。
   */
  private final boolean interpolation;
  private final UnloadedPosBehavior unloadedPosBehavior;
  /**
   * 是否允许突破方块数量限制。
   */
  private final boolean bypassLimit;
  private final @Nullable HistoryHolder historyHolder;
  private final @Nullable BlockTransformationHistory history;
  private final Iterator<@Nullable Runnable> runnables;
  /**
   * 转换过程中是否已遇到未加载的区域。
   */
  public boolean hasUnloadedPos = false;
  public @Nullable RegionSelection transformedRegionSelection;
  protected @Nullable RegionSelection oldActiveRegion;
  /**
   * 受影响的方块数量，包括转换后的新方块以及受影响的原方块。
   */
  private int affectedBlocks = 0;
  /**
   * 受影响的实体数量。
   */
  private int affectedEntities = 0;
  private Stage stage = Stage.PREPARATION;

  /**
   * @see Builder#build
   */
  private BlockTransformationTask(Component name, UUID uuid, CommandSourceStack source, boolean immediately, boolean shouldTransformRegion, Consumer<BlockTransformationTask> completionNotifier, Function<Vec3i, Vec3i> blockPosTransformer, @Nullable Function<Vec3, Vec3> posTransformer, @Nullable Function<Vec3, Vec3> invertedPosTransformer, Function<BlockState, BlockState> blockStateTransformer, @Nullable Consumer<Entity> entityTransformer, @Nullable Consumer<Entity> reverseEntityTransformer, Level world, Region region, ExecutionContext executionContext, BlockFunctionContext blockFunctionContext, @Nullable BlockPredicate affectsOnly, @Nullable BlockPredicate transformsOnly, @Nullable BlockFunction remaining, @Nullable Iterator<? extends Entity> entitiesToAffect, boolean interpolation, UnloadedPosBehavior unloadedPosBehavior, boolean bypassLimit, @Nullable HistoryHolder historyHolder, @Nullable BlockTransformationHistory history) {
    super(name, uuid, source);
    this.shouldTransformRegion = shouldTransformRegion;
    this.completionNotifier = completionNotifier;
    this.blockPosTransformer = blockPosTransformer;
    this.posTransformer = posTransformer;
    this.invertedPosTransformer = invertedPosTransformer;
    this.blockStateTransformer = blockStateTransformer;
    this.entityTransformer = entityTransformer;
    this.reverseEntityTransformer = reverseEntityTransformer;
    this.world = world;
    this.region = region;
    this.executionContext = executionContext;
    this.blockFunctionContext = blockFunctionContext;
    this.affectsOnly = affectsOnly;
    this.transformsOnly = transformsOnly;
    this.remaining = remaining;
    this.entitiesToAffect = entitiesToAffect;
    this.interpolation = interpolation;
    this.unloadedPosBehavior = unloadedPosBehavior;
    this.bypassLimit = bypassLimit;
    this.historyHolder = historyHolder;
    this.history = history;

    if (immediately) {
      this.runnables = taskSeries().getImmediateRunnables();
    } else {
      this.runnables = taskSeries().getSpeedAdjustedRunnables();
    }
  }

  public static Builder builder(Level world, Region region, Component name, UUID uuid, CommandSourceStack source) {
    return new Builder(world, region, name, uuid, source);
  }

  @Override
  public Component getName() {
    return Component.empty().append(super.getName()).append(" - ").append(Component.translatable("enhanced_commands.commands.transformation_stage", stage.ordinal(), stage.description));
  }

  @Override
  public boolean hasNext() {
    return runnables.hasNext();
  }

  @Override
  public @Nullable Runnable next() {
    return runnables.next();
  }

  public int getAffectedBlocks() {
    return affectedBlocks;
  }

  public int getAffectedEntities() {
    return affectedEntities;
  }

  /**
   * 检查区域是否可能存在未加载的位置，如果有，则抛出 {@link CommandSyntaxException}。只有在 {@code unloadedPosBehavior == REJECT} 时，才调用此方法。
   *
   * @throws CommandSyntaxException 区域可能存在未加载的位置时抛出。
   */
  public void checkAndRejectUnloadedPos() throws CommandSyntaxException {
    final BoundingBox box = region.minContainingBlockBox();
    if (box != null && !LoadUtil.isPosLoaded(world, box)) {
      throw SetReplaceBlocksCommand.UNLOADED_POS.create();
    }

    if (posTransformer != null) {
      final BoundingBox box2 = region.transformed(posTransformer).minContainingBlockBox();
      if (box2 != null && !LoadUtil.isPosLoaded(world, box2)) {
        throw SetReplaceBlocksCommand.UNLOADED_POS.create();
      }
    }
  }

  /**
   * 检查区域影响的方块数量是否可能超过限制，如果有，则抛出 {@link CommandSyntaxException}。只有在 {@code bypassLimit == false} 时，才调用此方法。
   *
   * @throws CommandSyntaxException 区域影响的方块数量可能超过限制时抛出。
   */
  public void checkAndRejectLimit() throws CommandSyntaxException {
    final int regionSizeLimit = BlockOperationConfig.current.regionSizeLimit;
    if (region.numberOfBlocksAffected() > regionSizeLimit) {
      throw SetReplaceBlocksCommand.REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), regionSizeLimit);
    }
  }

  public Iterable<@Nullable BlockPos> modifyIterableForUnloadedPos(Iterable<@Nullable BlockPos> iterable) {
    if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      return new BatchedFilterIterable<>(iterable, 16, (BlockPos blockPos) -> {
        @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) {
          hasUnloadedPos = true;
        }
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
      return Iterables.transform(iterable, blockPos -> {
        if (blockPos == null) {
          return null;
        }
        @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) {
          hasUnloadedPos = true;
          throw new UnloadedPosException(blockPos);
        }
        return blockPos;
      });
    }
    return iterable;
  }

  public TaskSeries taskSeries() {
    final ServerPlayer player = source.getPlayer();
    final Iterable<Runnable> preparation = List.of(() -> {
      try {
        if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
          checkAndRejectUnloadedPos();
        }
        if (!bypassLimit) {
          checkAndRejectLimit();
        }
      } catch (CommandSyntaxException e) {
        throw new CommandRuntimeException(e);
      }
    }, () -> {
      try {
        if (shouldTransformRegion && player != null && history != null) {
          oldActiveRegion = player.getActiveRegionOrThrow$ec();
        } else {
          oldActiveRegion = null;
        }
        if (oldActiveRegion != null && posTransformer != null && region.equals(oldActiveRegion.region())) {
          final RegionSelection clone = oldActiveRegion.clone();
          transformedRegionSelection = oldActiveRegion.transformed(posTransformer);
          oldActiveRegion = clone;
        } else {
          transformedRegionSelection = null;
        }
      } catch (CommandSyntaxException e) {
        throw new CommandRuntimeException(e);
      }
    });

    // 被转换走的方块在转换前的坐标
    final Long2ReferenceMap<@Nullable BlockState> posTransformedOut = new Long2ReferenceOpenHashMap<>();
    // 转换后的坐标和转换后的方块
    final Long2ReferenceMap<BlockState> transformedStates = new Long2ReferenceLinkedOpenHashMap<>();
    // 转换后的坐标和 NBT，NBT 一般不进行转换
    final Long2ReferenceMap<@Nullable CompoundTag> nbts = new Long2ReferenceOpenHashMap<>();

    final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

    final Iterable<@Nullable Runnable> storeTransformed;
    Iterable<@Nullable BlockPos> oldPosIterable = modifyIterableForUnloadedPos(region);

    storeTransformed = Iterables.transform(oldPosIterable, oldPos -> {
      if (oldPos == null) return null;
      final BlockInWorld blockInWorld = new BlockInWorld(world, oldPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
      if (transformsOnly == null || transformsOnly.test(blockInWorld, executionContext)) {
        final BlockState blockState = blockInWorld.getState();
        posTransformedOut.put(oldPos.asLong(), blockState);
        final BlockPos transformedBlockPos = mutable.set(blockPosTransformer.apply(oldPos));
        transformedStates.put(transformedBlockPos.asLong(), blockStateTransformer.apply(blockState));
        if (blockInWorld.getEntity() != null) {
          nbts.put(transformedBlockPos.asLong(), blockInWorld.getEntity().saveWithoutMetadata(world.registryAccess()));
        }

      } else {
        // 充 null 值表示未匹配到值，或者是未加载的区块。
        posTransformedOut.put(oldPos.asLong(), null);

      }
      return null;
    });

    final Iterable<@Nullable Runnable> collectMatchingTransformed;
    final LongSet matchingBlockPos;
    if (affectsOnly != null) {
      matchingBlockPos = new LongOpenHashSet();
      collectMatchingTransformed = () -> transformedStates.keySet().longStream().mapToObj(longValue -> (Runnable) () -> {
        mutable.set(longValue);
        if (affectsOnly.test(new BlockInWorld(world, mutable, unloadedPosBehavior == UnloadedPosBehavior.FORCE), executionContext)) {
          matchingBlockPos.add(longValue);
        }
      }).iterator();
    } else {
      matchingBlockPos = transformedStates.keySet();
      collectMatchingTransformed = Collections.emptyList();
    }

    Iterable<Long2ReferenceMap.Entry<BlockState>> releaseTransformedPos = () -> transformedStates.long2ReferenceEntrySet().iterator();
    if (affectsOnly != null) {
      releaseTransformedPos = Iterables.filter(releaseTransformedPos, entry -> matchingBlockPos.contains(entry.getLongKey()));
    }
    final Iterable<@Nullable Runnable> releaseTransformed = Iterables.transform(modifyIterableForUnloadedPos(Iterables.transform(releaseTransformedPos, input -> mutable.set(input.getLongKey()))),
        transformedBlockPos -> () -> {
          if (transformedBlockPos == null) return;
          final BlockState transformedState = transformedStates.get(transformedBlockPos.asLong());
          if (history != null) {
            history.recordBlockAndEntity(world, transformedBlockPos, transformedState);
          }
          boolean affected = MixinShared.setBlockStateWithModFlags(world, transformedBlockPos, transformedState, blockFunctionContext.flags, blockFunctionContext.modFlags);
          final CompoundTag nbtCompound = nbts.get(transformedBlockPos.asLong());
          final @Nullable BlockEntity transformedBlockEntity;
          if ((transformedBlockEntity = world.getBlockEntity(transformedBlockPos)) != null) {
            transformedBlockEntity.loadWithComponents(nbtCompound, world.registryAccess());
            affected = true;
          }
          if (affected) affectedBlocks++;
        });

    final Iterable<@Nullable Runnable> transformEntities = transformEntitiesEntry();

    final Iterable<@Nullable Runnable> collectMatchingRemaining;
    final Iterable<@Nullable Runnable> setRemaining;

    if (remaining != null) {
      final BatchedFilterIterable<@Nullable BlockPos> remainingPosIterable = new BatchedFilterIterable<>(region, 16, blockPos -> posTransformedOut.get(blockPos.asLong()) != null && !transformedStates.containsKey(blockPos.asLong()));
      if (affectsOnly != null) {
        final LongList affectedRemaining = new LongArrayList();
        collectMatchingRemaining = Iterables.transform(remainingPosIterable, blockPos -> () -> {
          if (blockPos != null && affectsOnly.test(new BlockInWorld(world, blockPos, false), executionContext)) {
            affectedRemaining.add(blockPos.asLong());
          }
        });
        setRemaining = () -> affectedRemaining.longStream().mapToObj(blockPos -> (Runnable) () -> {
          try {
            if (remaining.setBlock(world, mutable.set(blockPos), blockFunctionContext, null, history)) {
              affectedBlocks++;
            }
          } catch (CommandSyntaxException e) {
            throw new CommandRuntimeException(e);
          }
        }).iterator();
      } else {
        collectMatchingRemaining = Collections.emptyList();
        setRemaining = Iterables.transform(remainingPosIterable, blockPos -> () -> {
          try {
            if (blockPos != null && remaining.setBlock(world, blockPos, blockFunctionContext, null, history)) {
              affectedBlocks++;
            }
          } catch (CommandSyntaxException e) {
            throw new CommandRuntimeException(e);
          }
        });
      }
    } else {
      collectMatchingRemaining = setRemaining = Collections.emptyList();
    }

    final Iterable<@Nullable Runnable> addInterpolation;
    addInterpolation = addInterpolationEntry(transformedStates, posTransformedOut, nbts);


    final Iterable<Runnable> completion = Collections.singleton(() -> {
      if (transformedRegionSelection != null) {
        if (player != null) {
          if (history != null) {
            history.reverseEntities.add(Triple.of(player, Pair.of(
                player0 -> ((ServerPlayer) player0).setActiveRegion$ec(oldActiveRegion),
                player0 -> ((ServerPlayer) player0).setActiveRegion$ec(transformedRegionSelection)
            ), null));
          }
          player.setActiveRegion$ec(transformedRegionSelection);
        }
      }
      notifyUnloadedPos(unloadedPosBehavior, source);
      completionNotifier.accept(this);
    });

    return new TaskSeries(this, preparation, storeTransformed, collectMatchingTransformed, releaseTransformed, transformEntities, collectMatchingRemaining, setRemaining, addInterpolation, completion);
  }

  private Iterable<@Nullable Runnable> addInterpolationEntry(Long2ReferenceMap<BlockState> transformedStates, Long2ReferenceMap<@Nullable BlockState> posTransformedOut, Long2ReferenceMap<@Nullable CompoundTag> nbts) {
    final Iterable<@Nullable Runnable> addInterpolation;
    final BoundingBox untransformedBox;
    if (interpolation && posTransformer != null && invertedPosTransformer != null && (untransformedBox = region.minContainingBlockBox()) != null) {
      final List<BlockPos> transformedCorners = new ArrayList<>();
      untransformedBox.forAllCorners(blockPos -> transformedCorners.add(BlockPos.containing(posTransformer.apply(blockPos.getCenter()))));
      Iterable<@Nullable BlockPos> _posIterable = BlockPos.betweenClosed(transformedCorners.stream().mapToInt(Vec3i::getX).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getY).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getZ).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getX).max().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getY).max().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getZ).max().orElseThrow());
      _posIterable = modifyIterableForUnloadedPos(_posIterable);

      Iterable<@Nullable Runnable> unbatched = Iterables.transform(_posIterable, transformedPos -> {
        if (transformedPos == null) {
          return null;
        }
        final BlockPos invertedPos = BlockPos.containing(invertedPosTransformer.apply(transformedPos.getCenter()));
        if (region.contains(invertedPos) && !transformedStates.containsKey(transformedPos.asLong())) {
          if (affectsOnly == null || affectsOnly.test(new BlockInWorld(world, transformedPos, false), executionContext)) {
            final Optional<BlockPos> nearestOriginal = BlockPos.withinManhattanStream(invertedPos, 1, 1, 1).mapToLong(BlockPos::asLong).filter(posTransformedOut::containsKey).mapToObj(BlockPos::of).min(Comparator.comparingInt(o -> o.distManhattan(invertedPos)));
            if (nearestOriginal.isPresent()) {
              final long nearestOriginalLong = nearestOriginal.get().asLong();
              if (posTransformedOut.get(nearestOriginalLong) != null) {
                return () -> {
                  BlockState state = posTransformedOut.get(nearestOriginalLong);
                  if (state == null) {
                    state = Blocks.AIR.defaultBlockState();
                  }
                  if (history != null && !history.oldStates.containsKey(transformedPos.asLong())) {
                    history.recordBlockAndEntity(world, transformedPos, state);
                  }
                  boolean affected = MixinShared.setBlockStateWithModFlags(world, transformedPos, state, blockFunctionContext.flags, blockFunctionContext.modFlags);
                  final @Nullable CompoundTag nbtCompound = nbts.get(transformedPos.asLong());
                  final @Nullable BlockEntity transformedBlockEntity;
                  if (nbtCompound != null && (transformedBlockEntity = world.getBlockEntity(transformedPos)) != null) {
                    transformedBlockEntity.loadWithComponents(nbtCompound, world.registryAccess());
                    affected = true;
                  }
                  if (affected) affectedBlocks++;
                };
              }
            }
          }
        }
        return null;
      });
      addInterpolation = new BatchedFilterIterable<@Nullable Runnable>(unbatched, 16, Objects::nonNull);
    } else {
      addInterpolation = Collections.emptyList();
    }
    return addInterpolation;
  }

  private void notifyUnloadedPos(UnloadedPosBehavior unloadedPosBehavior, CommandSourceStack source) {
    if (hasUnloadedPos) {
      if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.broken").withStyle(Styles.ACTUAL), false);
      } else if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
        source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.setblocks.skipped").withStyle(Styles.ACTUAL), false);
      }
    }
  }

  private Iterable<@Nullable Runnable> transformEntitiesEntry() {
    final Iterable<@Nullable Runnable> transformEntities;
    if (entitiesToAffect != null) {
      transformEntities = Iterables.transform(() -> entitiesToAffect, entity -> (Runnable) () -> {
        if (history != null) {
          history.reverseEntities.add(Triple.of(entity, entityTransformer == null ? null : Pair.of(reverseEntityTransformer, entityTransformer), posTransformer != null ? entity.position() : null));
        }
        if (entityTransformer != null) {
          entityTransformer.accept(entity);
        }
        if (posTransformer != null) {
          final Vec3 transformedPos = posTransformer.apply(entity.position());
          if (entity instanceof ServerPlayer serverPlayerEntity) {
            serverPlayerEntity.connection.teleport(transformedPos.x, transformedPos.y, transformedPos.z, serverPlayerEntity.getYRot(), serverPlayerEntity.getXRot());
          } else {
            entity.teleportTo(transformedPos.x, transformedPos.y, transformedPos.z);
          }
        }
        affectedEntities++;
      });
    } else {
      transformEntities = Collections.emptyList();
    }
    return transformEntities;
  }

  public void addUndoableHistory() {
    if (history == null) return;
    if (historyHolder != null) {
      historyHolder.addUndoableHistory$ec(history);
    }
  }

  @Override
  public String toString() {
    return "Task [" + getName() + "]";
  }

  public enum Stage implements StringRepresentable {
    PREPARATION("preparation"),
    STORE_TRANSFORMED("store_transformed"),
    COLLECT_MATCHING_TRANSFORMED("collect_matching_transformed"),
    RELEASE_TRANSFORMED("release_transformed"),
    TRANSFORM_ENTITIES("transform_entities"),
    COLLECT_MATCHING_REMAINING("collect_matching_remaining"),
    SET_REMAINING("set_remaining"),
    ADD_INTERPOLATION("add_interpolation"),
    COMPLETION("completion");

    private final String name;
    public final Component description;

    Stage(String name) {
      this.name = name;
      this.description = Component.translatable("enhanced_commands.commands.transformation_stage." + name);
    }

    @Override
    public String getSerializedName() {
      return name;
    }
  }

  public record TaskSeries(BlockTransformationTask self, Iterable<Runnable> preparation, Iterable<@Nullable Runnable> storeTransformed, Iterable<@Nullable Runnable> collectMatchingTransformed, Iterable<@Nullable Runnable> releaseTransformed, Iterable<@Nullable Runnable> transformEntities, Iterable<@Nullable Runnable> collectMatchingRemaining, Iterable<@Nullable Runnable> setRemaining, Iterable<@Nullable Runnable> addInterpolation, Iterable<Runnable> completion) {
    // 使用 iterable 而非 iterator 是为了惰性计算，有些迭代器所使用的集合是在之前的迭代器中添加的，为了避免出现错误，应该在完成了添加集合元素之后，再调用集合的 iterator() 方法。
    private Iterable<@Nullable Runnable> markStage(Stage stage) {
      return Collections.singleton(() -> {
        self.stage = stage;
        LOGGER.info("Task [{}] stage {}: {}", self.getName(), stage.ordinal(), stage.getSerializedName());
      });
    }

    public Iterator<@Nullable Runnable> getSpeedAdjustedRunnables() {
      return Iterables.concat(
          markStage(Stage.PREPARATION),
          preparation,
          Collections.singleton(self::addUndoableHistory),
          markStage(Stage.STORE_TRANSFORMED),
          IterateUtils.batchAndSkip(storeTransformed, 16384, 1),
          markStage(Stage.COLLECT_MATCHING_TRANSFORMED),
          IterateUtils.batchAndSkip(collectMatchingTransformed, 16384, 1),
          markStage(Stage.RELEASE_TRANSFORMED),
          IterateUtils.batchAndSkip(releaseTransformed, 32768, 15),
          markStage(Stage.TRANSFORM_ENTITIES),
          IterateUtils.batchAndSkip(transformEntities, 16384, 7),
          markStage(Stage.COLLECT_MATCHING_REMAINING),
          IterateUtils.batchAndSkip(collectMatchingRemaining, 16384, 1),
          markStage(Stage.SET_REMAINING),
          IterateUtils.batchAndSkip(setRemaining, 32768, 15),
          markStage(Stage.ADD_INTERPOLATION),
          IterateUtils.batchAndSkip(addInterpolation, 32768, 15),
          markStage(Stage.COMPLETION),
          completion
      ).iterator();
    }

    public Iterator<@Nullable Runnable> getImmediateRunnables() {
      return UnloadedPosException.catching(Iterables.concat(
          preparation,
          Collections.singleton(self::addUndoableHistory),
          storeTransformed,
          collectMatchingTransformed,
          releaseTransformed,
          transformEntities,
          collectMatchingRemaining,
          setRemaining,
          addInterpolation,
          completion
      ).iterator());
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  public static final class Builder {
    private final Level world;
    private final Region region;
    private final Component name;
    private final UUID uuid;
    private final CommandSourceStack source;
    private @Nullable Function<Vec3i, Vec3i> blockPosTransformer;
    private @Nullable Function<Vec3, Vec3> posTransformer;
    private @Nullable Function<BlockState, BlockState> blockStateTransformer;
    private @Nullable Consumer<Entity> entityTransformer;
    private @Nullable Consumer<Entity> reverseEntityTransformer;
    private @Nullable ExecutionContext executionContext;
    private @Nullable BlockFunctionContext blockFunctionContext;
    private @Nullable BlockPredicate affectsOnly = null;
    private @Nullable BlockPredicate transformsOnly = null;
    private @Nullable BlockFunction remaining = DEFAULT_REMAINING_FUNCTION;
    private @Nullable Iterator<? extends Entity> entitiesToAffect = null;
    private @Nullable Function<Vec3, Vec3> invertedPosTransformer;
    private boolean interpolation = false;
    private UnloadedPosBehavior unloadedPosBehavior = UnloadedPosBehavior.REJECT;
    private boolean bypassLimit = false;
    private @Nullable BlockTransformationHistory history;
    private @Nullable HistoryHolder historyHolder;
    private boolean shouldTransformRegion;
    private @Nullable Consumer<BlockTransformationTask> completionNotifier;
    private boolean immediately = false;

    public Builder(Level world, Region region, Component name, UUID uuid, CommandSourceStack source) {
      this.world = world;
      this.region = region;
      this.name = name;
      this.uuid = uuid;
      this.source = source;
    }

    public Builder transformsBlockPos(Function<Vec3i, Vec3i> blockPosTransformer) {
      this.blockPosTransformer = blockPosTransformer;
      return this;
    }

    public Builder transformsPos(Function<Vec3, Vec3> posTransformer) {
      this.posTransformer = posTransformer;
      return this;
    }

    public Builder transformsPosBack(Function<Vec3, Vec3> invertedPosTransformer) {
      this.invertedPosTransformer = invertedPosTransformer;
      return this;
    }

    public Builder transformsBlockState(Function<BlockState, BlockState> blockStateTransformer) {
      this.blockStateTransformer = blockStateTransformer;
      return this;
    }

    public Builder transformsEntity(Consumer<Entity> entityTransformer, Consumer<Entity> reverseEntityTransformer) {
      this.entityTransformer = entityTransformer;
      this.reverseEntityTransformer = reverseEntityTransformer;
      return this;
    }

    public Builder shouldTransformRegion(boolean shouldTransformRegion) {
      this.shouldTransformRegion = shouldTransformRegion;
      return this;
    }

    public Builder notifiesCompletion(Consumer<BlockTransformationTask> completionNotifier) {
      this.completionNotifier = completionNotifier;
      return this;
    }

    public Builder setBlockPredicateContext(ExecutionContext executionContext) {
      this.executionContext = executionContext;
      return this;
    }

    public Builder setBlockFunctionContext(BlockFunctionContext blockFunctionContext) {
      this.blockFunctionContext = blockFunctionContext;
      return this;
    }

    public Builder affectsOnly(@Nullable BlockPredicate affectsOnly) {
      this.affectsOnly = affectsOnly;
      return this;
    }

    public Builder transformsOnly(@Nullable BlockPredicate transformsOnly) {
      this.transformsOnly = transformsOnly;
      return this;
    }

    public Builder fillRemainingWith(@Nullable BlockFunction remaining) {
      this.remaining = remaining;
      return this;
    }

    public Builder keepRemaining() {
      this.remaining = null;
      return this;
    }

    public Builder entitiesToAffect(@Nullable Iterator<? extends Entity> entitiesToAffect) {
      this.entitiesToAffect = entitiesToAffect;
      return this;
    }

    public Builder interpolates(boolean interpolation) {
      this.interpolation = interpolation;
      return this;
    }

    public Builder setUnloadedPosBehavior(UnloadedPosBehavior unloadedPosBehavior) {
      this.unloadedPosBehavior = unloadedPosBehavior;
      return this;
    }

    public Builder bypassLimit(boolean bypassLimit) {
      this.bypassLimit = bypassLimit;
      return this;
    }

    public Builder history(@Nullable HistoryHolder historyHolder, @Nullable BlockTransformationHistory history) {
      this.historyHolder = historyHolder;
      this.history = history;
      return this;
    }

    public Builder immediately(boolean immediately) {
      this.immediately = immediately;
      return this;
    }

    public BlockTransformationTask build() {
      Objects.requireNonNull(blockPosTransformer, "blockPosTransformer");
      Objects.requireNonNull(blockStateTransformer, "blockStateTransformer");
      Objects.requireNonNull(executionContext, "executionContext");
      Objects.requireNonNull(blockFunctionContext, "blockFunctionContext");
      Objects.requireNonNull(completionNotifier, "completionNotifier");
      return new BlockTransformationTask(name, uuid, source, immediately, shouldTransformRegion, completionNotifier, blockPosTransformer, posTransformer, invertedPosTransformer, blockStateTransformer, entityTransformer, reverseEntityTransformer, world, region, executionContext, blockFunctionContext, affectsOnly, transformsOnly, remaining, entitiesToAffect, interpolation, unloadedPosBehavior, bypassLimit, historyHolder, history);
    }
  }
}
