package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import pers.solid.ecmd.util.mixin.EnhancedItemInput;

@Mixin(GiveCommand.class)
public abstract class GiveCommandMixin {
  @WrapOperation(method = "giveItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/item/ItemInput;createItemStack(IZ)Lnet/minecraft/world/item/ItemStack;"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z")))
  private static ItemStack modifyCreateItemStack(ItemInput instance, int count, boolean allowOversizedStacks, Operation<ItemStack> original, @Local(ordinal = 0) ItemStack itemStack) {
    return instance instanceof EnhancedItemInput ? itemStack.copyWithCount(count) : itemStack;
  }
}
