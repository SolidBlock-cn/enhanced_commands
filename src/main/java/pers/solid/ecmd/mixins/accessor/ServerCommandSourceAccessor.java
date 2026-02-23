package pers.solid.ecmd.mixins.accessor;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CommandSourceStack.class)
public interface ServerCommandSourceAccessor {
  @Accessor
  boolean isSilent();

  @Accessor
  CommandSource getSource();

  @Accessor
  int getPermissionLevel();
}
