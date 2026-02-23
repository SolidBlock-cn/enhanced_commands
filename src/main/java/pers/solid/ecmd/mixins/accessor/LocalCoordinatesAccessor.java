package pers.solid.ecmd.mixins.accessor;

import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalCoordinates.class)
public interface LocalCoordinatesAccessor {
  @Accessor
  double getLeft();

  @Accessor
  double getUp();

  @Accessor
  double getForwards();
}
