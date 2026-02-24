package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityArgument.class)
public abstract class EntityArgumentMixin {
  @ModifyExpressionValue(method = "listSuggestions", at = @At(value = "NEW", target = "(Lcom/mojang/brigadier/StringReader;Z)Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;"))
  private EntitySelectorParser addContext(EntitySelectorParser entitySelectorReader, @Local(argsOnly = true) CommandContext<?> context) {
    entitySelectorReader.extension$ec().context = context;
    return entitySelectorReader;
  }
}
