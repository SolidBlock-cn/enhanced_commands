package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.command.argument.EntityArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;

@Mixin(EntityArgumentType.class)
public abstract class EntityArgumentTypeMixin {
  @ModifyExpressionValue(method = "listSuggestions", at = @At(value = "NEW", target = "(Lcom/mojang/brigadier/StringReader;Z)Lnet/minecraft/command/EntitySelectorReader;"))
  private static EntitySelectorReader addContext(EntitySelectorReader entitySelectorReader, @Local(argsOnly = true) CommandContext<?> context) {
    EntitySelectorReaderExtras.getOf(entitySelectorReader).context = context;
    return entitySelectorReader;
  }
}
