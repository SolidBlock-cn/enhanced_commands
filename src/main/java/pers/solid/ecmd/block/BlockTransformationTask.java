package pers.solid.ecmd.block;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.SimpleBlockFunction;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class BlockTransformationTask {
  public static final BlockFunction DEFAULT_REMAINING_FUNCTION = new SimpleBlockFunction(Blocks.AIR, Collections.emptyList());
  private final @NotNull Function<Vec3i, Vec3i> blockPosTransformer;
  private final @Nullable Function<Vec3d, Vec3d> posTransformer;
  private final @Nullable Function<Vec3d, Vec3d> invertedPosTransformer;
  private final @NotNull Function<BlockState, BlockState> blockStateTransformer;
  private final @Nullable Consumer<Entity> entityTransformer;
  private final @NotNull World world;
  private final @NotNull Region region;
  private final int flags;
  private final int modFlags;
  private final @Nullable BlockPredicate affectsOnly;
  private final @Nullable BlockPredicate transformsOnly;
  private final @Nullable BlockFunction remaining;
  private @Nullable Stream<? extends Entity> entitiesToAffect;
  private int affectedBlocks = 0;
  private int affectedEntities = 0;
  private final boolean interpolation;

  private BlockTransformationTask(@NotNull Function<Vec3i, Vec3i> blockPosTransformer, @Nullable Function<Vec3d, Vec3d> posTransformer, @Nullable Function<Vec3d, Vec3d> invertedPosTransformer, @NotNull Function<BlockState, BlockState> blockStateTransformer, @Nullable Consumer<Entity> entityTransformer, @NotNull World world, @NotNull Region region, int flags, int modFlags, @Nullable BlockPredicate affectsOnly, @Nullable BlockPredicate transformsOnly, @Nullable BlockFunction remaining, @Nullable Stream<? extends Entity> entitiesToAffect, boolean interpolation) {
    this.blockPosTransformer = blockPosTransformer;
    this.posTransformer = posTransformer;
    this.invertedPosTransformer = invertedPosTransformer;
    this.blockStateTransformer = blockStateTransformer;
    this.entityTransformer = entityTransformer;
    this.world = world;
    this.region = region;
    this.flags = flags;
    this.modFlags = modFlags;
    this.affectsOnly = affectsOnly;
    this.transformsOnly = transformsOnly;
    this.remaining = remaining;
    this.entitiesToAffect = entitiesToAffect;
    this.interpolation = interpolation;
  }

  public int getAffectedBlocks() {
    return affectedBlocks;
  }

  public int getAffectedEntities() {
    return affectedEntities;
  }

  public TaskSeries transformBlocks() {
    // 被转换走的方块在转换前的坐标
    final Map<BlockPos, BlockState> posTransformedOut = new HashMap<>();
    // 转换后的坐标和转换后的方块
    final Map<BlockPos, BlockState> transformedStates = new LinkedHashMap<>();
    // 转换后的坐标和 NBT，NBT 一般不进行转换
    final Map<BlockPos, NbtCompound> nbts = new HashMap<>();
    final Iterator<Void> storeTransformed = region.stream()
        .map(blockPos -> {
          final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, false);
          if (transformsOnly == null || transformsOnly.test(cachedBlockPosition)) {
            final BlockState blockState = cachedBlockPosition.getBlockState();
            posTransformedOut.put(blockPos.toImmutable(), blockState);
            final BlockPos transformedBlockPos = new BlockPos(blockPosTransformer.apply(blockPos));
            transformedStates.put(transformedBlockPos, blockStateTransformer.apply(blockState));
            if (cachedBlockPosition.getBlockEntity() != null) {
              nbts.put(transformedBlockPos, cachedBlockPosition.getBlockEntity().createNbt());
            }
          } else {
            posTransformedOut.put(blockPos.toImmutable(), null);
          }
          return (Void) null;
        })
        .iterator();

    final Iterator<Void> collectMatchingTransformed;
    final Set<BlockPos> matchingBlockPos;
    if (affectsOnly != null) {
      matchingBlockPos = new LinkedHashSet<>();
      collectMatchingTransformed = transformedStates.keySet().stream().map(blockPos -> {
        if (affectsOnly.test(new CachedBlockPosition(world, blockPos, false))) {
          matchingBlockPos.add(blockPos);
        }
        return (Void) null;
      }).iterator();
    } else {
      matchingBlockPos = transformedStates.keySet();
      collectMatchingTransformed = Collections.emptyIterator();
    }

    final Iterator<Void> releaseTransformed = transformedStates.entrySet().stream()
        .filter(affectsOnly == null ? Predicates.alwaysTrue() : entry -> matchingBlockPos.contains(entry.getKey()))
        .map(entry -> {
          final BlockPos transformedBlockPos = entry.getKey();
          final BlockState transformedState = entry.getValue();
          boolean affected = MixinSharedVariables.setBlockStateWithModFlags(world, transformedBlockPos, transformedState, flags, modFlags);
          final NbtCompound nbtCompound = nbts.get(transformedBlockPos);
          final @Nullable BlockEntity transformedBlockEntity;
          if (nbtCompound != null && (transformedBlockEntity = world.getBlockEntity(transformedBlockPos)) != null) {
            transformedBlockEntity.readNbt(nbtCompound);
            affected = true;
          }
          if (affected) affectedBlocks++;

          return (Void) null;
        }).iterator();

    final Iterator<Void> transformEntities;
    if (entitiesToAffect != null) {
      transformEntities = entitiesToAffect
          .map(entity -> {
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
            return (Void) null;
          }).iterator();
    } else {
      transformEntities = Collections.emptyIterator();
    }

    final Iterator<Void> collectMatchingRemaining;
    final Iterator<Void> setRemaining;
    if (remaining != null) {
      if (affectsOnly != null) {
        final List<BlockPos> affectedRemaining = new ArrayList<>();
        collectMatchingRemaining = region.stream()
            .filter(blockPos -> posTransformedOut.get(blockPos) != null && !transformedStates.containsKey(blockPos))
            .map(blockPos -> {
              if (affectsOnly.test(new CachedBlockPosition(world, blockPos, false))) {
                affectedRemaining.add(blockPos.toImmutable());
              }
              return (Void) null;
            }).iterator();
        setRemaining = affectedRemaining.stream()
            .map(blockPos -> {
              if (remaining.setBlock(world, blockPos, flags, modFlags)) {
                affectedBlocks++;
              }
              return (Void) null;
            }).iterator();
      } else {
        collectMatchingRemaining = Collections.emptyIterator();
        setRemaining = region.stream()
            .filter(blockPos -> posTransformedOut.get(blockPos) != null && !transformedStates.containsKey(blockPos))
            .map(blockPos -> {
              remaining.setBlock(world, blockPos, flags, modFlags);
              return (Void) null;
            }).iterator();
      }
    } else {
      collectMatchingRemaining = setRemaining = Collections.emptyIterator();
    }

    final BlockBox untransformedBox;
    final Iterator<Void> addInterpolation;
    if (interpolation && posTransformer != null && invertedPosTransformer != null && (untransformedBox = region.minContainingBlockBox()) != null) {
      final List<BlockPos> transformedCorners = new ArrayList<>();
      untransformedBox.forEachVertex(blockPos -> transformedCorners.add(BlockPos.ofFloored(posTransformer.apply(blockPos.toCenterPos()))));
      addInterpolation = BlockPos.stream(transformedCorners.stream().mapToInt(Vec3i::getX).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getY).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getZ).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getX).max().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getY).max().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getZ).max().orElseThrow()).filter(i -> {
        final BlockPos invertedPos = BlockPos.ofFloored(invertedPosTransformer.apply(i.toCenterPos()));
        if (region.contains(invertedPos) && !transformedStates.containsKey(i)) {
          if (affectsOnly == null || affectsOnly.test(new CachedBlockPosition(world, i, false))) {
            final Optional<BlockPos> nearestOriginal = BlockPos.streamOutwards(invertedPos, 1, 1, 1).filter(posTransformedOut::containsKey).map(BlockPos::toImmutable).min(Comparator.comparingInt(o -> o.getManhattanDistance(invertedPos)));
            if (nearestOriginal.isPresent() && posTransformedOut.get(nearestOriginal.get()) != null) {
              final BlockState state = posTransformedOut.get(nearestOriginal.get());
              boolean affected = MixinSharedVariables.setBlockStateWithModFlags(world, i, state, flags, modFlags);
              final NbtCompound nbtCompound = nbts.get(i);
              final @Nullable BlockEntity transformedBlockEntity;
              if (nbtCompound != null && (transformedBlockEntity = world.getBlockEntity(i)) != null) {
                transformedBlockEntity.readNbt(nbtCompound);
                affected = true;
              }
              if (affected) affectedBlocks++;
              return true;
            }
          }
        }
        return false;
      }).map(blockPos -> (Void) null).iterator();
    } else {
      addInterpolation = Collections.emptyIterator();
    }

    return new TaskSeries(storeTransformed, collectMatchingTransformed, releaseTransformed, transformEntities, collectMatchingRemaining, setRemaining, addInterpolation);
  }

  public static Builder builder(World world, Region region) {
    return new Builder(world, region);
  }

  public record TaskSeries(Iterator<Void> storeTransformed, Iterator<Void> collectMatchingTransformed, Iterator<Void> releaseTransformed, Iterator<Void> transformEntities, Iterator<Void> collectMatchingRemaining, Iterator<Void> setRemaining, Iterator<Void> addInterpolation) {
    public Iterator<Void> getSpeedAdjustedTask() {
      return Iterators.concat(
          IterateUtils.batchAndSkip(storeTransformed, 16384, 3),
          IterateUtils.batchAndSkip(collectMatchingTransformed, 16384, 3),
          IterateUtils.batchAndSkip(releaseTransformed, 32768, 15),
          IterateUtils.batchAndSkip(transformEntities, 16384, 7),
          IterateUtils.batchAndSkip(collectMatchingRemaining, 16384, 3),
          IterateUtils.batchAndSkip(setRemaining, 32768, 15),
          IterateUtils.batchAndSkip(addInterpolation, 32768, 15)
      );
    }

    public Iterator<Void> getImmediateTask() {
      return Iterators.concat(storeTransformed, collectMatchingTransformed, releaseTransformed, transformEntities, collectMatchingRemaining, setRemaining, addInterpolation);
    }
  }

  public static final class Builder {
    private final @NotNull World world;
    private final @NotNull Region region;
    private Function<Vec3i, Vec3i> blockPosTransformer;
    private @Nullable Function<Vec3d, Vec3d> posTransformer;
    private Function<BlockState, BlockState> blockStateTransformer;
    private @Nullable Consumer<Entity> entityTransformer;
    private int flags = 3;
    private int modFlags = 0;
    private @Nullable BlockPredicate affectsOnly = null;
    private @Nullable BlockPredicate transformsOnly = null;
    private @Nullable BlockFunction remaining = DEFAULT_REMAINING_FUNCTION;
    private @Nullable Stream<? extends Entity> entitiesToAffect = null;
    private Function<Vec3d, Vec3d> invertedPosTransformer;
    private boolean interpolation = false;

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

    public Builder transformsEntity(Consumer<Entity> entityTransformer) {
      this.entityTransformer = entityTransformer;
      return this;
    }

    public Builder setFlags(int flags) {
      this.flags = flags;
      return this;
    }

    public Builder setModFlags(int modFlags) {
      this.modFlags = modFlags;
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

    public Builder entitiesToAffect(@Nullable Stream<? extends Entity> entitiesToAffect) {
      this.entitiesToAffect = entitiesToAffect;
      return this;
    }

    public Builder interpolates(boolean interpolation) {
      this.interpolation = interpolation;
      return this;
    }

    public BlockTransformationTask build() {
      return new BlockTransformationTask(blockPosTransformer, posTransformer, invertedPosTransformer, blockStateTransformer, entityTransformer, world, region, flags, modFlags, affectsOnly, transformsOnly, remaining, entitiesToAffect, interpolation);
    }
  }
}
