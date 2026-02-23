package pers.solid.ecmd.regionselection;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class WandEvent {
  public static void registerEvents() {
    UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
      if (!player.isSpectator() && isWand(player.getItemInHand(hand))) {
        if (player instanceof ServerPlayer serverPlayer) {
          final Component text = serverPlayer.getOrResetRegionSelection$ec().clickSecondPoint(hitResult.getBlockPos().getCenter(), player).get();
          serverPlayer.syncActiveRegion$ec();
          if (text != null) player.sendSystemMessage(text);
        }
        return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
    });
    AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
      // 此方法在 ClientPlayerInteractionManager.updateBlockBreakingProgress 中不会调用，
      // 因为 ClientPlayerInteractionManagerMixin 会在此情况下进行阻止，
      // 从而避免点击一下左键却被多次调用的情况。
      if (!player.isSpectator() && isWand(player.getMainHandItem())) {
        if (player instanceof ServerPlayer serverPlayer) {
          final Component text = serverPlayer.getOrResetRegionSelection$ec().clickFirstPoint(pos.getCenter(), player).get();
          serverPlayer.syncActiveRegion$ec();
          if (text != null) player.sendSystemMessage(text);
        }
        return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
    });
  }

  public static ItemStack setWand(ItemStack stack) {
    CustomData.update(DataComponents.CUSTOM_DATA, stack, nbtCompound -> nbtCompound.putBoolean("enhanced_commands:region_selection_tool", true));
    stack.set(DataComponents.ITEM_NAME, Component.translatable("item.enhanced_commands.region_selection_tool").withStyle(style -> style.withColor(0xc7f0a2)));
    return stack;
  }

  public static ItemStack createWandStack() {
    return setWand(new ItemStack(Items.STICK));
  }

  public static boolean isWand(ItemStack stack) {
    final CustomData nbtComponent = stack.get(DataComponents.CUSTOM_DATA);
    return nbtComponent != null && nbtComponent.contains("enhanced_commands:region_selection_tool");
  }
}
