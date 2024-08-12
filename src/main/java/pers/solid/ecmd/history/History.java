package pers.solid.ecmd.history;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface History {
  @Contract(pure = true)
  @NotNull
  Text getName();

  @Nullable
  History undo(ServerCommandSource source, boolean immediately, boolean undoable);
}
