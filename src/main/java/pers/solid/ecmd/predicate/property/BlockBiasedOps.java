package pers.solid.ecmd.predicate.property;

import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.DelegatingOps;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.ApiStatus;

/**
 * <p>在包装了一层 {@link DynamicOps} 的基础上，额外存储一个 {@link #stateManager} 字段，以便于在特定情况下，需要在已知方块的 {@code stateManager}的情况下，序列化对应的属性名称，因为只有已经具体是哪个方块，才能根据属性名称获得到具体的属性对象，然后再根据此属性转化属性值。
 * <p>例如：</p>
 * <pre>{@code
 *    var ops = BlockBiasedOps.of(NbtOps.INSTANCE, Blocks.FURNACE.getStateManager());
 * }</pre>
 *
 * @see #of
 */
@ApiStatus.Experimental
public class BlockBiasedOps<T> extends DelegatingOps<T> {
  private final StateDefinition<Block, BlockState> stateManager;

  protected BlockBiasedOps(DynamicOps<T> delegate, StateDefinition<Block, BlockState> stateManager) {
    super(delegate);
    this.stateManager = stateManager;
  }

  public static <T> BlockBiasedOps<T> of(DynamicOps<T> delegate, StateDefinition<Block, BlockState> stateManager) {
    if (delegate instanceof BlockBiasedOps<T>) {
      throw new IllegalArgumentException("The delegate of " + BlockBiasedOps.class.getSimpleName() + " cannot be a " + BlockBiasedOps.class.getSimpleName() + "!");
    }
    return new BlockBiasedOps<>(delegate, stateManager);
  }

  public StateDefinition<Block, BlockState> getStateManager() {
    return stateManager;
  }
}
