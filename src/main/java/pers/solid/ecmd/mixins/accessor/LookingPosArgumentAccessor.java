package pers.solid.ecmd.mixins.accessor;

import net.minecraft.command.argument.LookingPosArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LookingPosArgument.class)
public interface LookingPosArgumentAccessor {
  @Accessor
  double getX();

  @Accessor
  double getY();

  @Accessor
  double getZ();
}
