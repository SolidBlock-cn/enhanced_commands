package pers.solid.ecmd.block;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionContext;
import pers.solid.ecmd.function.block.SimpleBlockFunction;
import pers.solid.ecmd.history.BlockTransformationHistory;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateContext;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.iterator.BatchedFilterIterable;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 涉及对区域内的方块（可能含有实体）进行变换的任务。此类能够处理一系列复杂的变换过程。
 */
public class BlockTransformationTask {
  public static final Logger LOGGER = LoggerFactory.getLogger(BlockTransformationTask.class);
  /**
   * 对变换后的原有的方块的处理方式，默认为直接使用空气填充。
   */
  public static final BlockFunction DEFAULT_REMAINING_FUNCTION = new SimpleBlockFunction(Blocks.AIR, Collections.emptyList());
  /**
   * 转换方块坐标的函数。每个方块的坐标都将根据此函数转换至新的坐标。
   */
  private final @NotNull Function<Vec3i, Vec3i> blockPosTransformer;
  /**
   * 转换坐标的函数。涉及到的实体将根据此函数转换至新的坐标。
   */
  private final @Nullable Function<Vec3d, Vec3d> posTransformer;
  /**
   * 反向转换坐标的函数，主要用于插值。
   */
  private final @Nullable Function<Vec3d, Vec3d> invertedPosTransformer;
  /**
   * 转换方块状态的函数，例如有些情况下需要将方块镜像或旋转。
   */
  private final @NotNull Function<BlockState, BlockState> blockStateTransformer;
  /**
   * 转换实体的 Consumer，不会另行返回值，例如有些情况下需要修改实体的水平朝向。
   */
  private final @Nullable Consumer<Entity> entityTransformer;
  private final @Nullable Consumer<Entity> reverseEntityTransformer;
  private final @NotNull World world;
  /**
   * 受操作影响的区域。
   */
  private final @NotNull Region region;
  private final BlockPredicateContext blockPredicateContext;
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
   * 受影响的方块数量，包括转换后的新方块以及受影响的原方块。
   */
  private int affectedBlocks = 0;
  /**
   * 受影响的实体数量。
   */
  private int affectedEntities = 0;
  /**
   * 转换过程中是否已遇到未加载的区域。
   */
  public boolean hasUnloadedPos = false;
  /**
   * 转换过程中是否进行插值。
   */
  private final boolean interpolation;
  private final @NotNull UnloadedPosBehavior unloadedPosBehavior;
  /**
   * 是否允许突破方块数量限制。
   */
  private final boolean bypassLimit;
  private final @Nullable CommandOutput historyTarget;
  private final @Nullable BlockTransformationHistory history;

  /**
   * @see Builder#build()
   */
  private BlockTransformationTask(@NotNull Function<Vec3i, Vec3i> blockPosTransformer, @Nullable Function<Vec3d, Vec3d> posTransformer, @Nullable Function<Vec3d, Vec3d> invertedPosTransformer, @NotNull Function<BlockState, BlockState> blockStateTransformer, @Nullable Consumer<Entity> entityTransformer, @Nullable Consumer<Entity> reverseEntityTransformer, @NotNull World world, @NotNull Region region, BlockPredicateContext blockPredicateContext, BlockFunctionContext blockFunctionContext, @Nullable BlockPredicate affectsOnly, @Nullable BlockPredicate transformsOnly, @Nullable BlockFunction remaining, @Nullable Iterator<? extends Entity> entitiesToAffect, boolean interpolation, @NotNull UnloadedPosBehavior unloadedPosBehavior, boolean bypassLimit, @Nullable CommandOutput historyTarget, @Nullable BlockTransformationHistory history) {
    this.blockPosTransformer = blockPosTransformer;
    this.posTransformer = posTransformer;
    this.invertedPosTransformer = invertedPosTransformer;
    this.blockStateTransformer = blockStateTransformer;
    this.entityTransformer = entityTransformer;
    this.reverseEntityTransformer = reverseEntityTransformer;
    this.world = world;
    this.region = region;
    this.blockPredicateContext = blockPredicateContext;
    this.blockFunctionContext = blockFunctionContext;
    this.affectsOnly = affectsOnly;
    this.transformsOnly = transformsOnly;
    this.remaining = remaining;
    this.entitiesToAffect = entitiesToAffect;
    this.interpolation = interpolation;
    this.unloadedPosBehavior = unloadedPosBehavior;
    this.bypassLimit = bypassLimit;
    this.historyTarget = historyTarget;
    this.history = history;
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
    final BlockBox box = region.minContainingBlockBox();
    if (box != null && !LoadUtil.isPosLoaded(world, box)) {
      throw FillReplaceCommand.UNLOADED_POS.create();
    }
  }

  /**
   * 检查区域影响的方块数量是否可能超过限制，如果有，则抛出 {@link CommandSyntaxException}。只有在 {@code bypassLimit == false} 时，才调用此方法。
   *
   * @throws CommandSyntaxException 区域影响的方块数量可能超过限制时抛出。
   */
  public void checkAndRejectLimit() throws CommandSyntaxException {
    if (region.numberOfBlocksAffected() > FillReplaceCommand.REGION_SIZE_LIMIT) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(region.numberOfBlocksAffected(), FillReplaceCommand.REGION_SIZE_LIMIT);
    }
  }

  public Iterable<@Nullable BlockPos> modifyIterableForUnloadedPos(Iterable<@Nullable BlockPos> iterable) {
    if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      return new BatchedFilterIterable<>(iterable, 16, blockPos -> {
        final boolean chunkLoaded = blockPos != null && world.isChunkLoaded(blockPos);
        if (!chunkLoaded) hasUnloadedPos = true;
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
      return Iterables.transform(iterable, blockPos -> {
        final boolean chunkLoaded = blockPos != null && world.isChunkLoaded(blockPos);
        if (!chunkLoaded) {
          hasUnloadedPos = true;
          throw new UnloadedPosException(blockPos);
        }
        return blockPos;
      });
    }
    return iterable;
  }

  public TaskSeries transformBlocks() throws CommandSyntaxException {
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      checkAndRejectUnloadedPos();
    }
    if (!bypassLimit) {
      checkAndRejectLimit();
    }

    // 被转换走的方块在转换前的坐标
    final Long2ReferenceMap<BlockState> posTransformedOut = new Long2ReferenceOpenHashMap<>();
    // 转换后的坐标和转换后的方块
    final Long2ReferenceMap<BlockState> transformedStates = new Long2ReferenceLinkedOpenHashMap<>();
    // 转换后的坐标和 NBT，NBT 一般不进行转换
    final Long2ReferenceMap<NbtCompound> nbts = new Long2ReferenceOpenHashMap<>();

    final BlockPos.Mutable mutable = new BlockPos.Mutable();

    final Iterable<Void> storeTransformed;
    {
      Iterable<@Nullable BlockPos> _posIterable = modifyIterableForUnloadedPos(region);
      _posIterable = Iterables.transform(_posIterable, blockPos -> {
        if (blockPos == null) return null;
        final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
        if ((transformsOnly == null || transformsOnly.test(cachedBlockPosition, blockPredicateContext)) && cachedBlockPosition.getBlockState() != null) {
          final BlockState blockState = cachedBlockPosition.getBlockState();
          posTransformedOut.put(blockPos.asLong(), blockState);
          final BlockPos transformedBlockPos = mutable.set(blockPosTransformer.apply(blockPos));
          transformedStates.put(transformedBlockPos.asLong(), blockStateTransformer.apply(blockState));
          if (cachedBlockPosition.getBlockEntity() != null) {
            nbts.put(transformedBlockPos.asLong(), cachedBlockPosition.getBlockEntity().createNbt(world.getRegistryManager()));
          }

          return transformedBlockPos;
        } else {
          // 充 null 值表示未匹配到值，或者是未加载的区块。
          posTransformedOut.put(blockPos.asLong(), null);

          return blockPos.toImmutable();
        }
      });
      storeTransformed = Iterables.transform(modifyIterableForUnloadedPos(_posIterable), blockPos -> null);
    }

    final Iterable<Void> collectMatchingTransformed;
    final LongSet matchingBlockPos;
    if (affectsOnly != null) {
      matchingBlockPos = new LongOpenHashSet();
      collectMatchingTransformed = () -> transformedStates.keySet().longStream().mapToObj(longValue -> {
        mutable.set(longValue);
        if (affectsOnly.test(new CachedBlockPosition(world, mutable, unloadedPosBehavior == UnloadedPosBehavior.FORCE), blockPredicateContext)) {
          matchingBlockPos.add(longValue);
        }
        return (Void) null;
      }).iterator();
    } else {
      matchingBlockPos = transformedStates.keySet();
      collectMatchingTransformed = Collections.emptyList();
    }

    Iterable<Long2ReferenceMap.Entry<BlockState>> releaseTransformedPos = () -> transformedStates.long2ReferenceEntrySet().iterator();
    if (affectsOnly != null) {
      releaseTransformedPos = Iterables.filter(releaseTransformedPos, entry -> matchingBlockPos.contains(entry.getLongKey()));
    }
    final Iterable<Void> releaseTransformed = Iterables.transform(releaseTransformedPos,
        entry -> {
          final BlockPos transformedBlockPos = mutable.set(entry.getLongKey());
          final BlockState transformedState = entry.getValue();
          if (history != null) {
            history.recordBlockAndEntity(world, transformedBlockPos, transformedState);
          }
          boolean affected = MixinShared.setBlockStateWithModFlags(world, transformedBlockPos, transformedState, blockFunctionContext.flags, blockFunctionContext.modFlags);
          final NbtCompound nbtCompound = nbts.get(entry.getLongKey());
          final @Nullable BlockEntity transformedBlockEntity;
          if (nbtCompound != null && (transformedBlockEntity = world.getBlockEntity(transformedBlockPos)) != null) {
            transformedBlockEntity.read(nbtCompound, world.getRegistryManager());
            affected = true;
          }
          if (affected) affectedBlocks++;

          return null;
        });

    final Iterable<Void> transformEntities = transformEntities();

    final Iterable<Void> collectMatchingRemaining;
    final Iterable<Void> setRemaining;

    if (remaining != null) {
      if (affectsOnly != null) {
        final LongList affectedRemaining = new LongArrayList();
        collectMatchingRemaining = Iterables.transform(
            new BatchedFilterIterable<>(region, 16, blockPos -> posTransformedOut.get(blockPos.asLong()) != null && !transformedStates.containsKey(blockPos.asLong())),
            blockPos -> {
              if (blockPos != null && affectsOnly.test(new CachedBlockPosition(world, blockPos, false), blockPredicateContext)) {
                affectedRemaining.add(blockPos.asLong());
              }
              return null;
            });
        setRemaining = () -> affectedRemaining.longStream()
            .mapToObj(blockPos -> {
              if (remaining.setBlock(world, mutable.set(blockPos), blockFunctionContext, history)) {
                affectedBlocks++;
              }
              return (Void) null;
            }).iterator();
      } else {
        collectMatchingRemaining = Collections.emptyList();
        setRemaining = Iterables.transform(
            new BatchedFilterIterable<>(region, 16, blockPos -> posTransformedOut.get(blockPos.asLong()) != null && !transformedStates.containsKey(blockPos.asLong())),
            blockPos -> {
              if (blockPos != null && remaining.setBlock(world, blockPos, blockFunctionContext, history)) {
                affectedBlocks++;
              }
              return null;
            });
      }
    } else {
      collectMatchingRemaining = setRemaining = Collections.emptyList();
    }

    final BlockBox untransformedBox;
    final Iterable<Void> addInterpolation;
    if (interpolation && posTransformer != null && invertedPosTransformer != null && (untransformedBox = region.minContainingBlockBox()) != null) {
      final List<BlockPos> transformedCorners = new ArrayList<>();
      untransformedBox.forEachVertex(blockPos -> transformedCorners.add(BlockPos.ofFloored(posTransformer.apply(blockPos.toCenterPos()))));
      Iterable<BlockPos> _posIterable = BlockPos.iterate(transformedCorners.stream().mapToInt(Vec3i::getX).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getY).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getZ).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getX).max().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getY).max().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getZ).max().orElseThrow());
      _posIterable = modifyIterableForUnloadedPos(_posIterable);
      _posIterable = new BatchedFilterIterable<>(_posIterable, 16,
          transformedPos -> {
            final BlockPos invertedPos = BlockPos.ofFloored(invertedPosTransformer.apply(transformedPos.toCenterPos()));
            if (region.contains(invertedPos) && !transformedStates.containsKey(transformedPos.asLong())) {
              if (affectsOnly == null || affectsOnly.test(new CachedBlockPosition(world, transformedPos, false), blockPredicateContext)) {
                final Optional<BlockPos> nearestOriginal = BlockPos.streamOutwards(invertedPos, 1, 1, 1).mapToLong(BlockPos::asLong).filter(posTransformedOut::containsKey).mapToObj(BlockPos::fromLong).min(Comparator.comparingInt(o -> o.getManhattanDistance(invertedPos)));
                if (nearestOriginal.isPresent()) {
                  final long nearestOriginalLong = nearestOriginal.get().asLong();
                  if (posTransformedOut.get(nearestOriginalLong) != null) {
                    final BlockState state = posTransformedOut.get(nearestOriginalLong);
                    if (history != null && !history.oldStates.containsKey(transformedPos.asLong())) {
                      history.recordBlockAndEntity(world, transformedPos, state);
                    }
                    boolean affected = MixinShared.setBlockStateWithModFlags(world, transformedPos, state, blockFunctionContext.flags, blockFunctionContext.modFlags);
                    final NbtCompound nbtCompound = nbts.get(transformedPos.asLong());
                    final @Nullable BlockEntity transformedBlockEntity;
                    if (nbtCompound != null && (transformedBlockEntity = world.getBlockEntity(transformedPos)) != null) {
                      transformedBlockEntity.read(nbtCompound, world.getRegistryManager());
                      affected = true;
                    }
                    if (affected) affectedBlocks++;
                    return true;
                  }
                }
              }
            }
            return false;
          });
      addInterpolation = Iterables.transform(_posIterable, blockPos -> null);
    } else {
      addInterpolation = Collections.emptyList();
    }

    return new TaskSeries(this, storeTransformed, collectMatchingTransformed, releaseTransformed, transformEntities, collectMatchingRemaining, setRemaining, addInterpolation);
  }

  private @NotNull Iterable<Void> transformEntities() {
    final Iterable<Void> transformEntities;
    if (entitiesToAffect != null) {
      transformEntities = Iterables.transform(() -> entitiesToAffect, entity -> {
        if (history != null) {
          history.reverseEntities.add(Triple.of(entity, entityTransformer == null ? null : Pair.of(reverseEntityTransformer, entityTransformer), posTransformer != null ? entity.getPos() : null));
        }
        if (entityTransformer != null) {
          entityTransformer.accept(entity);
        }
        if (posTransformer != null) {
          final Vec3d transformedPos = posTransformer.apply(entity.getPos());
          if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
            serverPlayerEntity.networkHandler.requestTeleport(transformedPos.x, transformedPos.y, transformedPos.z, serverPlayerEntity.getYaw(), serverPlayerEntity.getPitch(), PositionFlag.VALUES);
          } else {
            entity.requestTeleport(transformedPos.x, transformedPos.y, transformedPos.z);
          }
        }
        affectedEntities++;
        return (Void) null;
      });
    } else {
      transformEntities = Collections.emptyList();
    }
    return transformEntities;
  }

  public void addUndoableHistory() {
    if (history == null || !(historyTarget instanceof final HistoryHolder historyHolder)) return;
    historyHolder.addUndoableHistory$ec(history);
  }

  public static Builder builder(World world, Region region) {
    return new Builder(world, region);
  }

  public record TaskSeries(BlockTransformationTask self, Iterable<Void> storeTransformed, Iterable<Void> collectMatchingTransformed, Iterable<Void> releaseTransformed, Iterable<Void> transformEntities, Iterable<Void> collectMatchingRemaining, Iterable<Void> setRemaining, Iterable<Void> addInterpolation) {
    // 使用 iterable 而非 iterator 是为了惰性计算，有些迭代器所使用的集合是在之前的迭代器中添加的，为了避免出现错误，应该在完成了添加集合元素之后，再调用集合的 iterator() 方法。
    private Iterable<Void> logIterable(String message) {
      return IterateUtils.<Void>singletonPeekingStream(() -> LOGGER.info("Task {}: {}", self.region.asString(), message))::iterator;
    }

    public Iterator<Void> getSpeedAdjustedTask() {
      return UnloadedPosException.catching(Iterables.concat(
          () -> IterateUtils.singletonPeekingIterator(self::addUndoableHistory),
          logIterable("store_transformed"),
          IterateUtils.batchAndSkip(storeTransformed, 16384, 1),
          logIterable("collect_matching_transformed"),
          IterateUtils.batchAndSkip(collectMatchingTransformed, 16384, 1),
          logIterable("release_transformed"),
          IterateUtils.batchAndSkip(releaseTransformed, 32768, 15),
          logIterable("transform_entities"),
          IterateUtils.batchAndSkip(transformEntities, 16384, 7),
          logIterable("collect_matching_remaining"),
          IterateUtils.batchAndSkip(collectMatchingRemaining, 16384, 1),
          logIterable("set_remaining"),
          IterateUtils.batchAndSkip(setRemaining, 32768, 15),
          logIterable("add_interpolation"),
          IterateUtils.batchAndSkip(addInterpolation, 32768, 15)
      ).iterator());
    }

    public Iterator<Void> getImmediateTask() {
      return UnloadedPosException.catching(Iterables.concat(() -> IterateUtils.singletonPeekingIterator(self::addUndoableHistory), storeTransformed, collectMatchingTransformed, releaseTransformed, transformEntities, collectMatchingRemaining, setRemaining, addInterpolation).iterator());
    }
  }

  public static final class Builder {
    private final @NotNull World world;
    private final @NotNull Region region;
    private Function<Vec3i, Vec3i> blockPosTransformer;
    private @Nullable Function<Vec3d, Vec3d> posTransformer;
    private Function<BlockState, BlockState> blockStateTransformer;
    private @Nullable Consumer<Entity> entityTransformer;
    private @Nullable Consumer<Entity> reverseEntityTransformer;
    private BlockPredicateContext blockPredicateContext;
    private BlockFunctionContext blockFunctionContext;
    private @Nullable BlockPredicate affectsOnly = null;
    private @Nullable BlockPredicate transformsOnly = null;
    private @Nullable BlockFunction remaining = DEFAULT_REMAINING_FUNCTION;
    private @Nullable Iterator<? extends Entity> entitiesToAffect = null;
    private Function<Vec3d, Vec3d> invertedPosTransformer;
    private boolean interpolation = false;
    private @NotNull UnloadedPosBehavior unloadedPosBehavior = UnloadedPosBehavior.REJECT;
    private boolean bypassLimit = false;
    private @Nullable BlockTransformationHistory history;
    private CommandOutput historyTarget;

    public Builder(@NotNull World world, @NotNull Region region) {
      this.world = world;
      this.region = region;
    }

    public Builder transformsBlockPos(@NotNull Function<Vec3i, Vec3i> blockPosTransformer) {
      this.blockPosTransformer = blockPosTransformer;
      return this;
    }

    public Builder transformsPos(Function<Vec3d, Vec3d> posTransformer) {
      this.posTransformer = posTransformer;
      return this;
    }

    public Builder transformsPosBack(Function<Vec3d, Vec3d> invertedPosTransformer) {
      this.invertedPosTransformer = invertedPosTransformer;
      return this;
    }

    public Builder transformsBlockState(@NotNull Function<BlockState, BlockState> blockStateTransformer) {
      this.blockStateTransformer = blockStateTransformer;
      return this;
    }

    public Builder transformsEntity(Consumer<Entity> entityTransformer, Consumer<Entity> reverseEntityTransformer) {
      this.entityTransformer = entityTransformer;
      this.reverseEntityTransformer = reverseEntityTransformer;
      return this;
    }

    public Builder setBlockPredicateContext(BlockPredicateContext blockPredicateContext) {
      this.blockPredicateContext = blockPredicateContext;
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

    public Builder history(@Nullable CommandOutput historyTarget, @Nullable BlockTransformationHistory history) {
      this.historyTarget = historyTarget;
      this.history = history;
      return this;
    }

    public BlockTransformationTask build() {
      return new BlockTransformationTask(blockPosTransformer, posTransformer, invertedPosTransformer, blockStateTransformer, entityTransformer, reverseEntityTransformer, world, region, blockPredicateContext, blockFunctionContext, affectsOnly, transformsOnly, remaining, entitiesToAffect, interpolation, unloadedPosBehavior, bypassLimit, historyTarget, history);
    }
  }
}
