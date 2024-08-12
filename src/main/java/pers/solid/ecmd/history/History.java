package pers.solid.ecmd.history;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.extensions.IteratorTask;

public interface History {
  @Contract(pure = true)
  @NotNull
  Text getName();

  @NotNull
  Pair<? extends @Nullable IteratorTask<?>, ? extends @Nullable History> undo(ServerCommandSource source, boolean immediately, boolean undoable);
}
