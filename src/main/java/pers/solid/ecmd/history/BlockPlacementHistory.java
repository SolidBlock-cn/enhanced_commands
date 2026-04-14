package pers.solid.ecmd.history;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.iterator.IteratorTask;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.UUID;

public class BlockPlacementHistory implements History {
  public final Component name;
  public final ServerLevel world;
  public final int flag;
  public final int modFlag;
  public final Long2ObjectMap<BlockState> oldStates;
  public final Long2ObjectMap<CompoundTag> oldEntityData;
  public @Nullable IteratorTask<?> task;

  public BlockPlacementHistory(Component name, ServerLevel world, int flag, int modFlag) {
    this.name = name;
    this.world = world;
    this.flag = flag;
    this.modFlag = modFlag;
    oldStates = new Long2ObjectLinkedOpenHashMap<>();
    oldEntityData = new Long2ObjectOpenHashMap<>();
  }

  protected BlockPlacementHistory(Component name, ServerLevel world, int flag, int modFlag, Long2ObjectMap<BlockState> oldStates, Long2ObjectMap<CompoundTag> oldEntityData) {
    this.name = name;
    this.world = world;
    this.flag = flag;
    this.modFlag = modFlag;
    this.oldStates = oldStates;
    this.oldEntityData = oldEntityData;
  }

  @Override
  public Component getName() {
    return name;
  }

  @Override
  public Pair<? extends @Nullable IteratorTask<?>, ? extends @Nullable BlockPlacementHistory> undo(CommandSourceStack source, boolean immediately, boolean undoable) {
    if (task != null) {
      task.suspended = true;
    }
    final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    final MutableComponent undoName = Component.translatable("enhanced_commands.commands.undo.name", this.name);
    final @Nullable BlockPlacementHistory reverse = undoable ? new BlockPlacementHistory(undoName, world, flag, modFlag) : null;
    final Iterable<Void> iterable = Collections2.transform(oldStates.long2ObjectEntrySet(), entry -> {
      final long posLong = entry.getLongKey();
      mutable.set(posLong);
      final BlockState undoState = entry.getValue();
      if (reverse != null) {
        reverse.recordBlockAndEntity(world, mutable, undoState);
      }
      final BlockEntity oldBlockEntity = world.getBlockEntity(mutable);
      if (oldBlockEntity != null && !oldBlockEntity.isValidBlockState(undoState)) {
        world.removeBlockEntity(mutable);
      }
      MixinShared.setBlockStateWithModFlags(world, mutable, undoState, flag | Block.UPDATE_KNOWN_SHAPE & ~Block.UPDATE_NEIGHBORS, modFlag);
      final BlockEntity newBlockEntity = world.getBlockEntity(mutable);
      if (newBlockEntity != null && oldEntityData.containsKey(posLong)) {
        final CompoundTag undoEntityData = oldEntityData.get(posLong);
        newBlockEntity.loadWithComponents(undoEntityData, world.registryAccess());
      }
      return null;
    });

    if (immediately || oldStates.size() <= 16384) {
      IterateUtils.exhaust(iterable.iterator());
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.undo.finished", name), true);
      return Pair.of(null, reverse);
    } else {
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.undo.large_region", oldStates.size()).withStyle(ChatFormatting.YELLOW), true);

      final IteratorTask<Void> undoTask = new IteratorTask<>(undoName, UUID.randomUUID(), Iterators.concat(IterateUtils.batchAndSkip(iterable.iterator(), 32768, 15), IterateUtils.singletonPeekingIterator(() -> source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.undo.finished", name), true))));
      return Pair.of(undoTask, reverse);
    }
  }

  public void recordBlockAndEntity(Level world, BlockPos blockPos, @Nullable BlockState newState) {
    recordBlockAndEntity(world, blockPos, world.getBlockState(blockPos), newState);
  }

  public void recordBlockAndEntity(Level world, BlockPos blockPos, BlockState oldState, @Nullable BlockState newState) {
    if (oldState != null && !oldState.equals(newState)) {
      oldStates.put(blockPos.asLong(), oldState);
    }
    final BlockEntity oldEntity = world.getBlockEntity(blockPos);
    if (oldEntity != null) {
      oldEntityData.put(blockPos.asLong(), oldEntity.saveWithoutMetadata(world.registryAccess()));
    }
  }
}
