package pers.solid.ecmd.regionbuilder;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
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
          RegionBuilder regionBuilder = extension.ec$getRegionBuilder();
          if (regionBuilder == null) {
            extension.ec$setRegionBuilder(regionBuilder = extension.ec$getRegionBuilderType().createRegionBuilder());
          }
          regionBuilder.clickSecondPoint(hitResult.getBlockPos(), player);
          extension.ec$requireUpdateRegionBuilder();
        }
        return ActionResult.SUCCESS;
      }
      return ActionResult.PASS;
    });
    PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
      if (!player.isSpectator() && isWand(player.getMainHandStack())) {
        if (player instanceof ServerPlayerEntity) {
          final ServerPlayerEntityExtension extension = (ServerPlayerEntityExtension) player;
          RegionBuilder regionBuilder = extension.ec$getRegionBuilder();
          if (regionBuilder == null) {
            extension.ec$setRegionBuilder(regionBuilder = extension.ec$getRegionBuilderType().createRegionBuilder());
          }
          regionBuilder.clickFirstPoint(pos, player);
          extension.ec$requireUpdateRegionBuilder();
        }
        return false;
      }
      return true;
    });
  }

  public static ItemStack setWand(ItemStack stack) {
    stack.getOrCreateNbt().putBoolean("enhanced_commands:wand", true);
    stack.setCustomName(Text.translatable("enhancedCommands.argument.region_builder.wand").styled(style -> style.withColor(0xc7f0a2).withItalic(Boolean.FALSE)));
    return stack;
  }

  public static ItemStack createWandStack() {
    return setWand(new ItemStack(Items.STICK));
  }

  public static boolean isWand(ItemStack stack) {
    final NbtCompound nbt = stack.getNbt();
    return nbt != null && nbt.getBoolean("enhanced_commands:wand");
  }
}
