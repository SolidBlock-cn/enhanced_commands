package pers.solid.ecmd.mixins.accessor;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.BiFunction;

@Mixin(EntityAnchorArgument.Anchor.class)
public interface EntityAnchorAccessor {
  @Accessor
  BiFunction<Vec3, Entity, Vec3> getTransform();
}
