package pers.solid.ecmd.history;

import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.iterator.IteratorTask;

public interface History {
  @Contract(pure = true)
  @NotNull
  Component getName();

  @NotNull
  Pair<? extends @Nullable IteratorTask<?>, ? extends @Nullable History> undo(CommandSourceStack source, boolean immediately, boolean undoable);
}
