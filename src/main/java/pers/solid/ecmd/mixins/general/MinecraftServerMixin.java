package pers.solid.ecmd.mixins.general;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pers.solid.ecmd.history.History;
import pers.solid.ecmd.util.extension.HistoryHolder;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements HistoryHolder {
  @Unique
  private final Deque<History> enhanced_commands$undoableHistories = new ArrayDeque<>();
  @Unique
  private final Deque<History> enhanced_commands$redoableHistories = new ArrayDeque<>();

  @Override
  public Deque<History> getUndoableHistories$ec() {
    return enhanced_commands$undoableHistories;
  }

  @Override
  public Deque<History> getRedoableHistories$ec() {
    return enhanced_commands$redoableHistories;
  }
}
