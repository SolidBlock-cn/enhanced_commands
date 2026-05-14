package pers.solid.ecmd.history;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.iterator.ForwardingIteratorTask;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.iterator.IteratorTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BlockTransformationHistory extends BlockPlacementHistory {
  public final List<Triple<Entity, @Nullable Pair<Consumer<Entity>, Consumer<Entity>>, @Nullable Vec3>> reverseEntities = new ArrayList<>();

  public BlockTransformationHistory(Component name, ServerLevel world, int flag, int modFlag) {
    super(name, world, flag, modFlag);
  }

  public BlockTransformationHistory(Component name, ServerLevel world, int flag, int modFlag, Long2ObjectMap<BlockState> oldStates, Long2ObjectMap<CompoundTag> oldEntityData) {
    super(name, world, flag, modFlag, oldStates, oldEntityData);
  }

  @Override
  public Pair<? extends @Nullable IteratorTask, ? extends @Nullable BlockTransformationHistory> undo(CommandSourceStack source, boolean immediately, boolean undoable) {
    final var s = super.undo(source, immediately, undoable);
    final @Nullable IteratorTask superTask = s.getFirst();
    final @Nullable BlockPlacementHistory superHistory = s.getSecond();
    final BlockTransformationHistory redoHistory = superHistory == null ? null : new BlockTransformationHistory(superHistory.name, superHistory.world, superHistory.flag, superHistory.modFlag, superHistory.oldStates, superHistory.oldEntityData);

    Iterable<FailableRunnable<Throwable>> undoEntityTransformation = Iterables.transform(reverseEntities, entry -> () -> {
      final Entity entity = entry.getLeft();
      final var pair = entry.getMiddle();
      final Consumer<Entity> undo = pair == null ? null : pair.getFirst();
      final @Nullable Vec3 transformedPos = entry.getRight();
      @Nullable Vec3 oldPos = null;
      if (undo != null) {
        undo.accept(entity);
      }
      if (transformedPos != null) {
        oldPos = entity.position();
        if (entity instanceof ServerPlayer serverPlayerEntity) {
          serverPlayerEntity.connection.teleport(transformedPos.x, transformedPos.y, transformedPos.z, serverPlayerEntity.getYRot(), serverPlayerEntity.getXRot());
        } else {
          entity.teleportTo(transformedPos.x, transformedPos.y, transformedPos.z);
        }
      }
      if (redoHistory != null) {
        redoHistory.reverseEntities.add(Triple.of(entity, pair == null ? null : pair.swap(), oldPos));
      }
    });
    final IteratorTask redoTask = superTask == null ? null : new ForwardingIteratorTask(superTask.getName(), superTask.getUuid(), Iterators.concat(superTask, IterateUtils.batchAndSkip(undoEntityTransformation, 16384, 7).iterator()), source);
    if (superTask == null) {
      try {
        IterateUtils.exhaust(undoEntityTransformation.iterator());
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
    return Pair.of(redoTask, redoHistory);
  }
}
