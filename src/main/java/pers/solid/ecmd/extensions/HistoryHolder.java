package pers.solid.ecmd.extensions;

import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.config.BlockOperationConfig;
import pers.solid.ecmd.history.History;

import java.util.Deque;

/**
 * <p>承载操作历史记录的对象，通常是玩家。执行命令时，玩家操作时服务器通常会记录历史，历史通常会记录给执行命令的玩家，不随 {@code /execute as} 等命令的改变。
 * <p>默认情况下，每个对象承载的历史记录都是有限的。每进行一次操作，最后进行的操作添加在最后面。每次撤销或重做操作时，也是取最后的操作。当操作历史记录超过限制时，会删除最早添加的历史记录。
 */
public interface HistoryHolder {
  /**
   * @return 可撤销的历史记录，最后的操作添加在最后面。
   */
  @Contract(pure = true)
  Deque<History> getUndoableHistories$ec();

  /**
   * @return 可重做的历史记录（通常由撤销操作产生），最后的操作添加在最后面。
   */
  @Contract(pure = true)
  Deque<History> getRedoableHistories$ec();

  /**
   * 添加一次可撤销的操作记录，通常在执行了具体操作或者进行了一次重做操作时调用。历史记录的数量超过限制时，会移除最早的历史记录。
   *
   * @param history 可撤销的历史记录。
   */
  default void addUndoableHistory$ec(@NotNull History history) {
    addHistoryTo(history, getUndoableHistories$ec());
  }

  /**
   * 添加一次可还原的操作记录，通常在撤销了一次操作时调用。历史记录的数量超过限制时，会移除最早的历史记录。
   *
   * @param history 可重做的历史记录。
   */
  default void addRedoableHistory$ec(@NotNull History history) {
    addHistoryTo(history, getRedoableHistories$ec());
  }

  private void addHistoryTo(@NotNull History history, Deque<History> histories) {
    histories.addLast(history);
    final int exceeding = histories.size() - getMaxHistoryCount$ec();
    if (exceeding > 0) {
      for (int i = 0; i < exceeding; i++) {
        histories.removeFirst();
      }
    }
  }

  /**
   * @return 历史记录的最高数量。
   */
  default int getMaxHistoryCount$ec() {
    return BlockOperationConfig.current.maxHistoryCount;
  }

  default HistoryHolder inverse() {
    return new HistoryHolder() {
      @Override
      public Deque<History> getUndoableHistories$ec() {
        return HistoryHolder.this.getRedoableHistories$ec();
      }

      @Override
      public Deque<History> getRedoableHistories$ec() {
        return HistoryHolder.this.getUndoableHistories$ec();
      }
    };
  }

  static @Nullable HistoryHolder fromSource(CommandSourceStack source) {
    if (source.getPlayer() instanceof HistoryHolder historyHolder) {
      return historyHolder;
    } else if (source.getServer() instanceof HistoryHolder historyHolder) {
      return historyHolder;
    } else {
      return null;
    }
  }
}
