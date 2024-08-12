package pers.solid.ecmd.history;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.mixin.MixinShared;

public class BlockPlacementHistory implements History {
  public final Text name;
  public final ServerWorld world;
  public final int flag;
  public final int modFlag;
  public final Long2ObjectMap<BlockState> oldStates = new Long2ObjectArrayMap<>();
  public final Long2ObjectMap<NbtCompound> oldEntityData = new Long2ObjectOpenHashMap<>();
  public @Nullable IteratorTask<?> task;

  public BlockPlacementHistory(Text name, ServerWorld world, int flag, int modFlag) {
    this.name = name;
    this.world = world;
    this.flag = flag;
    this.modFlag = modFlag;
  }

  @Override
  public @NotNull Text getName() {
    return name;
  }

  @Override
  public @Nullable History undo(ServerCommandSource source, boolean immediately, boolean undoable) {
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
        final BlockState oldState = world.getBlockState(mutable);
        if (!oldState.equals(undoState)) {
          reverse.oldStates.put(posLong, oldState);
        }
        final BlockEntity oldEntity = world.getBlockEntity(mutable);
        if (oldEntity != null) {
          reverse.oldEntityData.put(posLong, oldEntity.createNbt(world.getRegistryManager()));
        }
      }
      MixinShared.setBlockStateWithModFlags(world, mutable, undoState, flag, modFlag);
      final BlockEntity blockEntity = world.getBlockEntity(mutable);
      if (blockEntity != null && oldEntityData.containsKey(posLong)) {
        final NbtCompound undoEntityData = oldEntityData.get(posLong);
        blockEntity.read(undoEntityData, world.getRegistryManager());
      }
      return null;
    });

    if (immediately || oldStates.size() <= 16384) {
      IterateUtils.exhaust(iterable.iterator());
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.undo.finished", name), true);
    } else {
      source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.undo.large_region", oldStates.size()).formatted(Formatting.YELLOW), true);

      ((ThreadExecutorExtension) world.getServer()).addIteratorTask$ec(name, Iterators.concat(IterateUtils.batchAndSkip(iterable.iterator(), 32768, 15), IterateUtils.singletonPeekingIterator(() -> source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.undo.finished", name), true))));
    }
    return reverse;
  }
}
