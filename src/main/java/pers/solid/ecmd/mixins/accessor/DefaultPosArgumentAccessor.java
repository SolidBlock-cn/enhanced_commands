package pers.solid.ecmd.mixins.accessor;

import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.DefaultPosArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DefaultPosArgument.class)
public interface DefaultPosArgumentAccessor {
  @Accessor
  CoordinateArgument getX();

  @Accessor
  CoordinateArgument getY();

  @Accessor
  CoordinateArgument getZ();
}
