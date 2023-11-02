package pers.solid.ecmd.regionselection;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

public final class WandEvent {
  public static void registerEvents() {
    UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
      if (!player.isSpectator() && isWand(player.getStackInHand(hand))) {
        if (player instanceof ServerPlayerEntity) {
          final ServerPlayerEntityExtension extension = (ServerPlayerEntityExtension) player;
          final Text text = extension.ec$getOrResetRegionSelection().clickSecondPoint(hitResult.getBlockPos(), player).get();
          if (text != null) player.sendMessage(text);
        }
        return ActionResult.SUCCESS;
      }
      return ActionResult.PASS;
    });
    AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
      // 此方法在 ClientPlayerInteractionManager.updateBlockBreakingProgress 中不会调用，
      // 因为 ClientPlayerInteractionManagerMixin 会在此情况下进行阻止，
      // 从而避免点击一下左键却被多次调用的情况。
      if (!player.isSpectator() && isWand(player.getMainHandStack())) {
        if (player instanceof ServerPlayerEntity) {
          final ServerPlayerEntityExtension extension = (ServerPlayerEntityExtension) player;
          final Text text = extension.ec$getOrResetRegionSelection().clickFirstPoint(pos, player).get();
          if (text != null) player.sendMessage(text);
        }
        return ActionResult.SUCCESS;
      }
      return ActionResult.PASS;
    });
  }

  public static ItemStack setWand(ItemStack stack) {
    stack.getOrCreateNbt().putBoolean("enhanced_commands:region_selection_tool", true);
    stack.setCustomName(Text.translatable("item.enhanced_commands.region_selection_tool").styled(style -> style.withColor(0xc7f0a2).withItalic(Boolean.FALSE)));
    return stack;
  }

  public static ItemStack createWandStack() {
    return setWand(new ItemStack(Items.STICK));
  }

  public static boolean isWand(ItemStack stack) {
    final NbtCompound nbt = stack.getNbt();
    return nbt != null && nbt.getBoolean("enhanced_commands:region_selection_tool");
  }
}
