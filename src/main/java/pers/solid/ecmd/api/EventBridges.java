package pers.solid.ecmd.api;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 本模组中使用的 {@link EventBridge}，既包括将 Fabric API 和 NeoForge 的已有事件连接起来的事件，也有此模组特有的事件。
 *
 * @see #INSTANCE
 * @see ClientEventBridges
 */
@MethodsReturnNonnullByDefault
public interface EventBridges {
  EventBridges INSTANCE = getInstance();

  /**
   * @see #INSTANCE
   */
  @ExpectPlatform
  static EventBridges getInstance() {
    throw new AssertionError();
  }

  /**
   * @return 右键点击方块（使用方块）的事件。
   */
  EventBridge<UseBlockCallbackBridge> useBlockEvent();

  /**
   * @return 左键点击方块（攻击方块）的事件。
   *
   */
  EventBridge<AttackBlockCallbackBridge> attackBlockEvent();

  /**
   * 将方块进行上下翻转的事件，用于 {@code /mirror} 等命令。
   *
   * @return 上下翻转方块的事件。
   */
  EventBridge<FlipStateCallback> flipState();

  interface UseBlockCallbackBridge {
    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult);
  }

  interface AttackBlockCallbackBridge {
    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction);
  }
}
