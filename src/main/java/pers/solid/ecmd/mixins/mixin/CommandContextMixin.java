package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.mixins.ext.ServerCommandSourceExtension;

@Mixin(CommandContext.class)
public abstract class CommandContextMixin<S> {
  @Shadow
  @Final
  private S source;

  /**
   * @see pers.solid.ecmd.command.ModCommands#REGION_ARGUMENTS_MODIFIER
   */
  @ModifyExpressionValue(method = "getArgument", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
  public Object getAddedArgument(Object original, @Local(argsOnly = true) String name) {
    if (original == null && source instanceof ServerCommandSourceExtension extension) {
      return new ParsedArgument<>(0, 0, extension.getExtraArguments$ec().get(name));
    }

    return original;
  }
}
