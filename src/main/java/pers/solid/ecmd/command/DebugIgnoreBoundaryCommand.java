package pers.solid.ecmd.command;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.mixins.mixin.GuiMixin;
import pers.solid.ecmd.mixins.mixin.LevelMixin;
import pers.solid.ecmd.mixins.mixin.PlayerMixin;
import pers.solid.ecmd.mixins.mixin.ServerGamePacketListenerImplMixin;
import pers.solid.ecmd.util.TextUtil;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public enum DebugIgnoreBoundaryCommand implements CommandRegistrationCallback {
  INSTANCE;

  /**
   * 忽略世界界限。
   *
   * @see PlayerMixin#noClampPos(Player, double, double, double)
   * @see ServerGamePacketListenerImplMixin#noClampHorizontal(double, CallbackInfoReturnable)
   * @see ServerGamePacketListenerImplMixin#noClampVertical(double, CallbackInfoReturnable)
   * @see LevelMixin#forceValidHorizontally(BlockPos, CallbackInfoReturnable)
   * @see LevelMixin#forceValidVertically(int, CallbackInfoReturnable)
   * @see pers.solid.ecmd.mixins.mixin.EntityMixin#noClampWhenUpdating(double, double, double, Operation)
   */
  public static boolean ignoreBoundary = false;
  /**
   * 忽视世界边界。
   *
   * @see GuiMixin#skipBorderWarning(double)
   * @see pers.solid.ecmd.mixins.mixin.WorldBorderMixin
   */
  public static boolean ignoreBorder = false;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {
    dispatcher.register(literal("debug:ignoreboundary")
        .executes(context -> {
          context.getSource().sendFeedback$ecBridge(() -> Component.literal("ignore boundary: ").append(TextUtil.wrapBoolean(ignoreBoundary)), false);
          return ignoreBoundary ? 1 : 0;
        })
        .then(argument("value", bool())
            .executes(context -> {
              final boolean value = getBool(context, "value");
              ignoreBoundary = value;
              context.getSource().sendFeedback$ecBridge(() -> Component.literal("ignore boundary set to ").append(TextUtil.wrapBoolean(value)), true);
              return value ? 1 : 0;
            })));
    dispatcher.register(literal("debug:ignoreborder")
        .executes(context -> {
          context.getSource().sendFeedback$ecBridge(() -> Component.literal("ignore boundary: ").append(TextUtil.wrapBoolean(ignoreBorder)), false);
          return ignoreBorder ? 1 : 0;
        })
        .then(argument("value", bool())
            .executes(context -> {
              final boolean value = getBool(context, "value");
              ignoreBorder = value;
              context.getSource().sendFeedback$ecBridge(() -> Component.literal("ignore boundary set to ").append(TextUtil.wrapBoolean(value)), true);
              return value ? 1 : 0;
            })));
  }
}
