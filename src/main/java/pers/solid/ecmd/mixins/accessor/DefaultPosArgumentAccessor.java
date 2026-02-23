package pers.solid.ecmd.mixins.accessor;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldCoordinates.class)
public interface DefaultPosArgumentAccessor {
  @Accessor
  WorldCoordinate getX();

  @Accessor
  WorldCoordinate getY();

  @Accessor
  WorldCoordinate getZ();
}
