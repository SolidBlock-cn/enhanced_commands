package pers.solid.ecmd.api.neoforge;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

/**
 * 对方块进行上下翻转的事件的 NeoForge 实现。
 *
 * @see pers.solid.ecmd.api.FlipStateCallback
 */
public class FlipStateEvent extends Event {
  /**
   * 执行过程中的方块状态，可能随着事件的执行而被修改，此字段影响最终的结果。
   */
  private @NotNull BlockState intermediateState;
  /**
   * 执行此事件前的最初的方块状态。
   */
  private final @NotNull BlockState originalState;

  public FlipStateEvent(@NotNull BlockState blockState) {
    this.intermediateState = blockState;
    this.originalState = blockState;
  }

  public @NotNull BlockState getIntermediateState() {
    return intermediateState;
  }

  /**
   * 执行整个翻转方块操作前的方块状态，如果注册了多个事件，那么此字段的值不会因先前执行 {@link #setIntermediateState(BlockState)} 而修改。
   */
  public @NotNull BlockState getOriginalState() {
    return originalState;
  }

  public void setIntermediateState(@NotNull BlockState intermediateState) {
    this.intermediateState = intermediateState;
  }

}
