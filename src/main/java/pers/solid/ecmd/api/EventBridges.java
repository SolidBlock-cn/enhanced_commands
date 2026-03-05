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

@MethodsReturnNonnullByDefault
public interface EventBridges {
  EventBridges INSTANCE = getInstance();

  @ExpectPlatform
  static EventBridges getInstance() {
    throw new AssertionError();
  }

  EventBridge<UseBlockCallbackBridge> useBlockEvent();

  EventBridge<AttackBlockCallbackBridge> attackBlockEvent();

  interface UseBlockCallbackBridge {
    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult);
  }

  interface AttackBlockCallbackBridge {
    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction);
  }
}
