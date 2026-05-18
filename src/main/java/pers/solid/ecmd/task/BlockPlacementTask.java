package pers.solid.ecmd.task;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.UnloadedPosException;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.BlockFunctionContext;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.enums.UnloadedPosBehavior;
import pers.solid.ecmd.util.iterator.AbstractIteratorTask;
import pers.solid.ecmd.util.iterator.BatchedFilterIterable;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

/**
 * 执行方块放置（包括替换）操作的任务，不包括诸如移动等方块变换操作。
 */
public class BlockPlacementTask extends AbstractIteratorTask {
  protected final ServerLevel world;
  private final BlockFunction blockFunction;
  protected @Nullable
  final BlockPredicate predicate;
  private final boolean immediately;
  private final BlockFunctionContext context;
  private final UnloadedPosBehavior unloadedPosBehavior;
  /**
   * 影响的方块的数量。
   */
  public int numbersAffected = 0;
  /**
   * 在执行过程中，是否遇到了未加载的区域。
   */
  public boolean hasUnloaded = false;
  public final Iterable<@Nullable BlockPos> posIterable;
  protected final @Nullable BlockPlacementHistory history;
  protected Iterator<@Nullable Runnable> runnables;

  protected BlockPlacementTask(Component name, UUID uuid, CommandSourceStack source, ServerLevel world, Region region, BlockFunction blockFunction, @Nullable BlockPredicate predicate, boolean immediately, BlockFunctionContext context, UnloadedPosBehavior unloadedPosBehavior, boolean undoable) {
    super(name, uuid, source);
    this.world = world;
    this.blockFunction = blockFunction;
    this.predicate = predicate;
    this.immediately = immediately;
    this.context = context;
    this.unloadedPosBehavior = unloadedPosBehavior;
    this.posIterable = prepareActualPosIterable(region, unloadedPosBehavior);
    this.history = undoable ? new BlockPlacementHistory(name, world, context.flags, context.modFlags) : null;
    this.runnables = getCombinedRunnables().iterator();
  }

  @Override
  public boolean hasNext() {
    return runnables.hasNext();
  }

  @Override
  public @Nullable Runnable next() {
    return runnables.next();
  }

  private Iterable<@Nullable BlockPos> prepareActualPosIterable(Region region, UnloadedPosBehavior unloadedPosBehavior) {
    if (unloadedPosBehavior == UnloadedPosBehavior.SKIP) {
      return new BatchedFilterIterable<>(region, 16, blockPos -> {
        @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) hasUnloaded = true;
        return chunkLoaded;
      });
    } else if (unloadedPosBehavior == UnloadedPosBehavior.BREAK) {
      return Iterables.transform(region, blockPos -> {
        @SuppressWarnings("deprecation") final boolean chunkLoaded = world.hasChunkAt(blockPos);
        if (!chunkLoaded) {
          hasUnloaded = true;
          throw new UnloadedPosException(blockPos);
        }
        return blockPos;
      });
    } else {
      return region;
    }
  }

  protected final Long2ObjectMap<BlockState> oldStates = new Long2ObjectLinkedOpenHashMap<>();

  public Iterable<@Nullable Runnable> step1CollectOldStates() {
    if (predicate == null) {
      return Iterables.transform(posIterable, blockPos -> () -> {
        if (blockPos == null) {
          return;
        }
        oldStates.put(blockPos.asLong(), world.getBlockState(blockPos));
      });
    } else {
      return Iterables.transform(posIterable, blockPos -> () -> {
        if (blockPos == null) {
          return;
        }
        final BlockInWorld blockInWorld = new BlockInWorld(world, blockPos, unloadedPosBehavior == UnloadedPosBehavior.FORCE);
        blockInWorld.getState();
        if (predicate.test(blockInWorld, context)) {
          oldStates.put(blockPos.asLong(), blockInWorld.getState());
        }
      });
    }
  }

  public Iterable<@Nullable Runnable> step2PlaceBlocks() {
    final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    return Iterables.transform(oldStates.long2ObjectEntrySet(), entry -> () -> {
      try {
        if (blockFunction.setBlock(world, mutable.set(entry.getLongKey()), context, entry.getValue(), history)) {
          numbersAffected++;
        }
      } catch (CommandSyntaxException e) {
        throw new CommandRuntimeException(e);
      }
    });
  }

  public Iterable<@Nullable Runnable> step3FinalClaim() {
    return Collections.singleton(() -> source.sendFeedback$ecBridge(() -> hasUnloaded ? switch (unloadedPosBehavior) {
      case SKIP -> Component.translatable("enhanced_commands.commands.setblocks.complete_skipped", numbersAffected);
      case BREAK -> Component.translatable("enhanced_commands.commands.setblocks.complete_broken", numbersAffected);
      default -> Component.translatable("enhanced_commands.commands.setblocks.complete", numbersAffected);
    } : Component.translatable("enhanced_commands.commands.setblocks.complete", numbersAffected).enhanced$$(), true));
  }

  public Iterable<@Nullable Runnable> getCombinedRunnables() {
    final Iterable<@Nullable Runnable> step1CollectOldStates = step1CollectOldStates();
    final Iterable<@Nullable Runnable> step2PlaceBlocks = step2PlaceBlocks();
    final Iterable<@Nullable Runnable> step3FinalClaim = step3FinalClaim();
    if (immediately) {
      return Iterables.concat(step1CollectOldStates, step2PlaceBlocks, step3FinalClaim);
    } else {
      return Iterables.concat(
          IterateUtils.batchAndSkip(step1CollectOldStates, 16384, 1),
          IterateUtils.batchAndSkip(step2PlaceBlocks, 32768, 15),
          step3FinalClaim
      );
    }
  }

  public static Builder builder(Component name, UUID uuid, CommandSourceStack source) {
    return new Builder(name, uuid, source);
  }

  public static class Builder {
    private final Component name;
    private final UUID uuid;
    private final CommandSourceStack source;

    public Builder undoable(boolean undoable) {
      this.undoable = undoable;
      return this;
    }

    public Builder unloadedPosBehavior(@Nullable UnloadedPosBehavior unloadedPosBehavior) {
      this.unloadedPosBehavior = unloadedPosBehavior;
      return this;
    }

    public Builder blockFunctionContext(@Nullable BlockFunctionContext context) {
      this.context = context;
      return this;
    }

    public Builder immediately(boolean immediately) {
      this.immediately = immediately;
      return this;
    }

    public Builder blockPredicate(@Nullable BlockPredicate predicate) {
      this.predicate = predicate;
      return this;
    }

    public Builder blockFunction(@Nullable BlockFunction blockFunction) {
      this.blockFunction = blockFunction;
      return this;
    }

    public Builder region(@Nullable Region region) {
      this.region = region;
      return this;
    }

    public Builder world(@Nullable ServerLevel world) {
      this.world = world;
      return this;
    }

    private @Nullable ServerLevel world;
    private @Nullable Region region;
    private @Nullable BlockFunction blockFunction;
    private @Nullable BlockPredicate predicate;
    private boolean immediately;
    private @Nullable BlockFunctionContext context;
    private @Nullable UnloadedPosBehavior unloadedPosBehavior;
    private boolean undoable;

    public Builder(Component name, UUID uuid, CommandSourceStack source) {
      this.name = name;
      this.uuid = uuid;
      this.source = source;
    }

    public BlockPlacementTask build() {
      Objects.requireNonNull(world, "world");
      Objects.requireNonNull(region, "region");
      Objects.requireNonNull(blockFunction, "blockFunction");
      Objects.requireNonNull(context, "BlockFunctionContext");
      Objects.requireNonNull(unloadedPosBehavior, "unloadedPosBehavior");
      return new BlockPlacementTask(name, uuid, source, world, region, blockFunction, predicate, immediately, context, unloadedPosBehavior, undoable);
    }
  }
}
