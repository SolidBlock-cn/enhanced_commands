package pers.solid.ecmd.util.mixin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.command.FillReplaceCommand;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

public final class MixinSharedVariables {
  public static final ImmutableMap<String, GameMode> EXTENDED_GAME_MODE_NAMES = ImmutableMap.of(
      "s", GameMode.SURVIVAL,
      "c", GameMode.CREATIVE,
      "a", GameMode.ADVENTURE,
      "sp", GameMode.SPECTATOR,
      "0", GameMode.SURVIVAL,
      "1", GameMode.CREATIVE,
      "2", GameMode.ADVENTURE,
      "3", GameMode.SPECTATOR);
  /**
   * 如果此值为 {@code true}，那么会抑制 {@link net.minecraft.world.chunk.WorldChunk#setBlockState(BlockPos, BlockState, boolean)} 对 {@link BlockState#onBlockAdded(World, BlockPos, BlockState, boolean)} 的调用。通常来说，这是一个临时的设置，在调用前修改此值，调用后立即复原，以免对其他模组产生影响。
   *
   * @see pers.solid.ecmd.mixin.WorldChunkMixin#wrappedOnBlockAdded(BlockState, World, BlockPos, BlockState, boolean)
   */
  public static boolean suppressOnBlockAdded = false;

  /**
   * 如果此值为 {@code true}，那么会抑制 {@link net.minecraft.world.chunk.WorldChunk#setBlockState(BlockPos, BlockState, boolean)} 对 {@link BlockState#onStateReplaced(World, BlockPos, BlockState, boolean)} 的调用。通常来说，这是一个临时的设置，在调用前修改此值，调用后立即复原，以免对其他模组产生影响。
   */
  public static boolean suppressOnStateReplaced = false;

  public static void implementModFlag(int modFlags) {
    MixinSharedVariables.suppressOnBlockAdded = (modFlags & FillReplaceCommand.SUPPRESS_INITIAL_CHECK_FLAG) != 0;
    MixinSharedVariables.suppressOnStateReplaced = (modFlags & FillReplaceCommand.SUPPRESS_REPLACED_CHECK_FLAG) != 0;
  }

  public static void releaseModFlag() {
    MixinSharedVariables.suppressOnBlockAdded = false;
    MixinSharedVariables.suppressOnStateReplaced = false;
  }

  public static boolean setBlockStateWithModFlags(World world, BlockPos blockPos, BlockState blockState, int flags, int modFlags) {
    MixinSharedVariables.implementModFlag(modFlags);
    boolean result;
    try {
      result = world.setBlockState(blockPos, blockState, flags);
    } finally {
      MixinSharedVariables.releaseModFlag();
    }
    return result;
  }

  private static Reference<CommandRegistryAccess> commandRegistryAccessReference;

  /**
   * 在注册命令时调用此方法，以设置 {@link #commandRegistryAccessReference} 的值，注意它是个弱引用，通过来说在服务器关闭或者离开世界之前都不应该清除。
   *
   * @see pers.solid.ecmd.mixin.CommandManagerMixin
   * @see CommandManager#CommandManager
   */
  public static void setWeakCommandRegistryAccess(CommandRegistryAccess commandRegistryAccess) {
    commandRegistryAccessReference = new WeakReference<>(commandRegistryAccess);
  }

  /**
   * 对于自身不会在 {@link CommandRegistryAccess} 的参数类型，调用此方法，以获取当前注册命令时所使用的 {@link CommandRegistryAccess}。如果没有注册命令，或者已经被清除，则返回备用值并发出警告。
   */
  public static CommandRegistryAccess getCommandRegistryAccess() {
    if (commandRegistryAccessReference != null) {
      final CommandRegistryAccess commandRegistryAccess = commandRegistryAccessReference.get();
      if (commandRegistryAccess != null) {
        return commandRegistryAccess;
      }
    }
    if (commandRegistryAccessReference == null) {
      EnhancedCommands.LOGGER.warn("Enhanced Commands mod: There is no CommandRegistryAccess object stored, which should not have happened. Is it called before commands are registered?");
    } else {
      EnhancedCommands.LOGGER.warn("Enhanced Commands mod: The CommandRegistryAccess object seems removed as garbage, which should not have happened. Is is called after the server closes or player leaves world?");
    }
    final CommandRegistryAccess backup = CommandManager.createRegistryAccess(DynamicRegistryManager.of(Registries.REGISTRIES));
    commandRegistryAccessReference = new SoftReference<>(backup);
    return backup;
  }

  private MixinSharedVariables() {
  }
}
