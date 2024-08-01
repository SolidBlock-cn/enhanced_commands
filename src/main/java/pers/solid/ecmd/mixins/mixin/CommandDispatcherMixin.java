package pers.solid.ecmd.mixins.mixin;

import com.mojang.brigadier.CommandDispatcher;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = CommandDispatcher.class, remap = false)
public abstract class CommandDispatcherMixin {
}
