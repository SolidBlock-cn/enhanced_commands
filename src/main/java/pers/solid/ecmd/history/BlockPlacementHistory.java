package pers.solid.ecmd.history;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.UUID;

public class BlockPlacementHistory implements History {
  public final Text name;
  public final ServerWorld world;
  public final int flag;
  public final int modFlag;
  public final Long2ObjectMap<BlockState> oldStates;
  public final Long2ObjectMap<NbtCompound> oldEntityData;
  public @Nullable IteratorTask<?> task;

  public BlockPlacementHistory(Text name, ServerWorld world, int flag, int modFlag) {
    this.name = name;
    this.world = world;
    this.flag = flag;
    this.modFlag = modFlag;
    oldStates = new Long2ObjectLinkedOpenHashMap<>();
    oldEntityData = new Long2ObjectOpenHashMap<>();
  }

  protected BlockPlacementHistory(Text name, ServerWorld world, int flag, int modFlag, Long2ObjectMap<BlockState> oldStates, Long2ObjectMap<NbtCompound> oldEntityData) {
    this.name = name;
    this.world = world;
    this.flag = flag;
    this.modFlag = modFlag;
    this.oldStates = oldStates;
    this.oldEntityData = oldEntityData;
  }

  @Override
  public @NotNull Text getName() {
    return name;
  }

  @Override
  public @NotNull Pair<? extends @Nullable IteratorTask<?>, ? extends @Nullable BlockPlacementHistory> undo(ServerCommandSource source, boolean immediately, boolean undoable) {
    if (task != null) {
      task.suspended = true;
    }
    final BlockPos.Mutable mutable = new BlockPos.Mutable();
    final MutableText undoName = Text.translatable("enhanced_commands.commands.undo.name", this.name);
    final @Nullable BlockPlacementHistory reverse = undoable ? new BlockPlacementHistory(undoName, world, flag, modFlag) : null;
    final Iterable<Void> iterable = Collections2.transform(oldStates.long2ObjectEntrySet(), entry -> {
      final long posLong = entry.getLongKey();
      mutable.set(posLong);
      final BlockState undoState = entry.getValue();
      if (reverse != null) {
        reverse.recordBlockAndEntity(world, mutable, undoState);
      }
      final BlockEntity oldBlockEntity = world.getBlockEntity(mutable);
      if (oldBlockEntity != null && !oldBlockEntity.supports(undoState)) {
        world.removeBlockEntity(mutable);
      }
      MixinShared.setBlockStateWithModFlags(world, mutable, undoState, flag | Block.FORCE_STATE & ~Block.NOTIFY_NEIGHBORS, modFlag);
      final BlockEntity newBlockEntity = world.getBlockEntity(mutable);
      if (newBlockEntity != null && oldEntityData.containsKey(posLong)) {
        final NbtCompound undoEntityData = oldEntityData.get(posLong);
        newBlockEntity.read(undoEntityData, world.getRegistryManager());
      }
      return null;
    });

    if (immediately || oldStates.size() <= 16384) {
      IterateUtils.exhaust(iterable.iterator());
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.undo.finished", name), true);
      return Pair.of(null, reverse);
    } else {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.undo.large_region", oldStates.size()).formatted(Formatting.YELLOW), true);

      final IteratorTask<Void> undoTask = new IteratorTask<>(undoName, UUID.randomUUID(), Iterators.concat(IterateUtils.batchAndSkip(iterable.iterator(), 32768, 15), IterateUtils.singletonPeekingIterator(() -> source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.undo.finished", name), true))));
      return Pair.of(undoTask, reverse);
    }
  }

  public void recordBlockAndEntity(World world, BlockPos blockPos, @Nullable BlockState newState) {
    recordBlockAndEntity(world, blockPos, world.getBlockState(blockPos), newState);
  }

  public void recordBlockAndEntity(World world, BlockPos blockPos, BlockState oldState, @Nullable BlockState newState) {
    if (oldState != null && !oldState.equals(newState)) {
      oldStates.put(blockPos.asLong(), oldState);
    }
    final BlockEntity oldEntity = world.getBlockEntity(blockPos);
    if (oldEntity != null) {
      oldEntityData.put(blockPos.asLong(), oldEntity.createNbt(world.getRegistryManager()));
    }
  }
}
