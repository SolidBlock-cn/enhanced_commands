package pers.solid.ecmd.block;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.*;
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
import pers.solid.ecmd.command.FillReplaceCommand;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.SimpleBlockFunction;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.LoadUtil;
import pers.solid.ecmd.util.UnloadedPosBehavior;
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
  private @Nullable Iterator<? extends Entity> entitiesToAffect;
  private int affectedBlocks = 0;
  private int affectedEntities = 0;
  public boolean hasUnloadedPos = false;
  private final boolean interpolation;
  private final @NotNull UnloadedPosBehavior unloadedPosBehavior;

  private BlockTransformationTask(@NotNull Function<Vec3i, Vec3i> blockPosTransformer, @Nullable Function<Vec3d, Vec3d> posTransformer, @Nullable Function<Vec3d, Vec3d> invertedPosTransformer, @NotNull Function<BlockState, BlockState> blockStateTransformer, @Nullable Consumer<Entity> entityTransformer, @NotNull World world, @NotNull Region region, int flags, int modFlags, @Nullable BlockPredicate affectsOnly, @Nullable BlockPredicate transformsOnly, @Nullable BlockFunction remaining, @Nullable Iterator<? extends Entity> entitiesToAffect, boolean interpolation, @NotNull UnloadedPosBehavior unloadedPosBehavior) {
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
    this.unloadedPosBehavior = unloadedPosBehavior;
  }

  public int getAffectedBlocks() {
    return affectedBlocks;
  }

  public int getAffectedEntities() {
    return affectedEntities;
  }

  public void checkAndRejectUnloadedPos() throws CommandSyntaxException {
    final BlockBox box = region.minContainingBlockBox();
    if (box != null && !LoadUtil.isPosLoaded(world, box)) {
      throw FillReplaceCommand.UNLOADED_POS.create();
    }
  }

  public void checkAndRejectUnloadedPos(Stream<BlockPos> stream) throws CommandSyntaxException {
    if (stream.anyMatch(blockPos -> !world.isChunkLoaded(blockPos))) {
      throw FillReplaceCommand.UNLOADED_POS.create();
    }
  }

  public Stream<BlockPos> modifyStream(Stream<BlockPos> stream) {
    if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      stream = stream.filter(blockPos -> {
        final boolean chunkLoaded = world.isChunkLoaded(blockPos);
        if (!chunkLoaded) hasUnloadedPos = true;
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
      stream = stream.peek(blockPos -> {
        final boolean chunkLoaded = world.isChunkLoaded(blockPos);
        if (!chunkLoaded) {
          hasUnloadedPos = true;
          throw new UnloadedPosException(blockPos);
        }
      });
    }
    return stream;
  }

  public TaskSeries transformBlocks() throws CommandSyntaxException {
    if (unloadedPosBehavior == UnloadedPosBehavior.REJECT) {
      checkAndRejectUnloadedPos();
    }

    // 被转换走的方块在转换前的坐标
    final Long2ReferenceMap<BlockState> posTransformedOut = new Long2ReferenceOpenHashMap<>();
    // 转换后的坐标和转换后的方块
    final Long2ReferenceMap<BlockState> transformedStates = new Long2ReferenceLinkedOpenHashMap<>();
    // 转换后的坐标和 NBT，NBT 一般不进行转换
    final Long2ReferenceMap<NbtCompound> nbts = new Long2ReferenceOpenHashMap<>();

    final BlockPos.Mutable mutable = new BlockPos.Mutable();
    final Iterable<Void> storeTransformed = () -> modifyStream(modifyStream(region.stream())
        .map(blockPos -> {
          final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
          if ((transformsOnly == null || transformsOnly.test(cachedBlockPosition)) && cachedBlockPosition.getBlockState() != null) {
            final BlockState blockState = cachedBlockPosition.getBlockState();
            posTransformedOut.put(blockPos.asLong(), blockState);
            final BlockPos transformedBlockPos = mutable.set(blockPosTransformer.apply(blockPos));
            transformedStates.put(transformedBlockPos.asLong(), blockStateTransformer.apply(blockState));
            if (cachedBlockPosition.getBlockEntity() != null) {
              nbts.put(transformedBlockPos.asLong(), cachedBlockPosition.getBlockEntity().createNbt());
            }

            return transformedBlockPos;
          } else {
            // 充 null 值表示未匹配到值，或者是未加载的区块。
            posTransformedOut.put(blockPos.asLong(), null);

            return blockPos.toImmutable();
          }
        }))
        .map(blockPos -> (Void) null).iterator();

    final Iterable<Void> collectMatchingTransformed;
    final LongSet matchingBlockPos;
    if (affectsOnly != null) {
      matchingBlockPos = new LongOpenHashSet();
      collectMatchingTransformed = () -> transformedStates.keySet().longStream().mapToObj(longValue -> {
        mutable.set(longValue);
        if (affectsOnly.test(new CachedBlockPosition(world, mutable, unloadedPosBehavior == UnloadedPosBehavior.FORCE))) {
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
          boolean affected = MixinSharedVariables.setBlockStateWithModFlags(world, transformedBlockPos, transformedState, flags, modFlags);
          final NbtCompound nbtCompound = nbts.get(entry.getLongKey());
          final @Nullable BlockEntity transformedBlockEntity;
          if (nbtCompound != null && (transformedBlockEntity = world.getBlockEntity(transformedBlockPos)) != null) {
            transformedBlockEntity.readNbt(nbtCompound);
            affected = true;
          }
          if (affected) affectedBlocks++;

          return null;
        });

    final Iterable<Void> transformEntities;
    if (entitiesToAffect != null) {
      transformEntities = Iterables.transform(() -> entitiesToAffect, entity -> {
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

    final Iterable<Void> collectMatchingRemaining;
    final Iterable<Void> setRemaining;
    if (remaining != null) {
      if (affectsOnly != null) {
        final LongList affectedRemaining = new LongArrayList();
        collectMatchingRemaining = () -> region.stream()
            .filter(blockPos -> posTransformedOut.get(blockPos.asLong()) != null && !transformedStates.containsKey(blockPos.asLong()))
            .map(blockPos -> {
              if (affectsOnly.test(new CachedBlockPosition(world, blockPos, false))) {
                affectedRemaining.add(blockPos.asLong());
              }
              return (Void) null;
            }).iterator();
        setRemaining = () -> affectedRemaining.longStream()
            .mapToObj(blockPos -> {
              if (remaining.setBlock(world, mutable.set(blockPos), flags, modFlags)) {
                affectedBlocks++;
              }
              return (Void) null;
            }).iterator();
      } else {
        collectMatchingRemaining = Collections.emptyList();
        setRemaining = () -> region.stream()
            .filter(blockPos -> posTransformedOut.get(blockPos.asLong()) != null && !transformedStates.containsKey(blockPos.asLong()))
            .map(blockPos -> {
              if (remaining.setBlock(world, blockPos, flags, modFlags)) {
                affectedBlocks++;
              }
              return (Void) null;
            }).iterator();
      }
    } else {
      collectMatchingRemaining = setRemaining = Collections.emptyList();
    }

    final BlockBox untransformedBox;
    final Iterable<Void> addInterpolation;
    if (interpolation && posTransformer != null && invertedPosTransformer != null && (untransformedBox = region.minContainingBlockBox()) != null) {
      final List<BlockPos> transformedCorners = new ArrayList<>();
      untransformedBox.forEachVertex(blockPos -> transformedCorners.add(BlockPos.ofFloored(posTransformer.apply(blockPos.toCenterPos()))));
      addInterpolation = () -> BlockPos.stream(transformedCorners.stream().mapToInt(Vec3i::getX).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getY).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getZ).min().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getX).max().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getY).max().orElseThrow(), transformedCorners.stream().mapToInt(Vec3i::getZ).max().orElseThrow())
          .filter(i -> {
            final BlockPos invertedPos = BlockPos.ofFloored(invertedPosTransformer.apply(i.toCenterPos()));
            if (region.contains(invertedPos) && !transformedStates.containsKey(i.asLong())) {
              if (affectsOnly == null || affectsOnly.test(new CachedBlockPosition(world, i, false))) {
                final Optional<BlockPos> nearestOriginal = BlockPos.streamOutwards(invertedPos, 1, 1, 1).mapToLong(BlockPos::asLong).filter(posTransformedOut::containsKey).mapToObj(BlockPos::fromLong).min(Comparator.comparingInt(o -> o.getManhattanDistance(invertedPos)));
                if (nearestOriginal.isPresent()) {
                  final long nearestOriginalLong = nearestOriginal.get().asLong();
                  if (posTransformedOut.get(nearestOriginalLong) != null) {
                    final BlockState state = posTransformedOut.get(nearestOriginalLong);
                    boolean affected = MixinSharedVariables.setBlockStateWithModFlags(world, i, state, flags, modFlags);
                    final NbtCompound nbtCompound = nbts.get(i.asLong());
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
            }
            return false;
          }).map(blockPos -> (Void) null).iterator();
    } else {
      addInterpolation = Collections.emptyList();
    }

    return new TaskSeries(storeTransformed, collectMatchingTransformed, releaseTransformed, transformEntities, collectMatchingRemaining, setRemaining, addInterpolation);
  }

  public static Builder builder(World world, Region region) {
    return new Builder(world, region);
  }

  public record TaskSeries(Iterable<Void> storeTransformed, Iterable<Void> collectMatchingTransformed, Iterable<Void> releaseTransformed, Iterable<Void> transformEntities, Iterable<Void> collectMatchingRemaining, Iterable<Void> setRemaining, Iterable<Void> addInterpolation) {
    // 使用 iterable 而非 iterator 是为了惰性计算，有些迭代器所使用的集合是在之前的迭代器中添加的，为了避免出现错误，应该在完成了添加集合元素之后，再调用集合的 iterator() 方法。
    public Iterator<Void> getSpeedAdjustedTask() {
      return UnloadedPosException.catching(Iterables.concat(
          IterateUtils.batchAndSkip(storeTransformed, 16384, 1),
          IterateUtils.batchAndSkip(collectMatchingTransformed, 16384, 1),
          IterateUtils.batchAndSkip(releaseTransformed, 32768, 15),
          IterateUtils.batchAndSkip(transformEntities, 16384, 7),
          IterateUtils.batchAndSkip(collectMatchingRemaining, 16384, 1),
          IterateUtils.batchAndSkip(setRemaining, 32768, 15),
          IterateUtils.batchAndSkip(addInterpolation, 32768, 15)
      ).iterator());
    }

    public Iterator<Void> getImmediateTask() {
      return UnloadedPosException.catching(Iterables.concat(storeTransformed, collectMatchingTransformed, releaseTransformed, transformEntities, collectMatchingRemaining, setRemaining, addInterpolation).iterator());
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
    private @Nullable Iterator<? extends Entity> entitiesToAffect = null;
    private Function<Vec3d, Vec3d> invertedPosTransformer;
    private boolean interpolation = false;
    private @NotNull UnloadedPosBehavior unloadedPosBehavior = UnloadedPosBehavior.REJECT;

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

    public BlockTransformationTask build() {
      return new BlockTransformationTask(blockPosTransformer, posTransformer, invertedPosTransformer, blockStateTransformer, entityTransformer, world, region, flags, modFlags, affectsOnly, transformsOnly, remaining, entitiesToAffect, interpolation, unloadedPosBehavior);
    }
  }
}
