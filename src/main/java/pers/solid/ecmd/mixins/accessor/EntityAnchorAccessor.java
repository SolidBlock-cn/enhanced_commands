package pers.solid.ecmd.mixins.accessor;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.BiFunction;

@Mixin(EntityAnchorArgumentType.EntityAnchor.class)
public interface EntityAnchorAccessor {
  @Accessor
  BiFunction<Vec3d, Entity, Vec3d> getOffset();
}
