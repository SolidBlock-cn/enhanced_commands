package pers.solid.ecmd.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntitySelectorReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntitySelectorReader.class)
public interface EntitySelectorReaderAccessor {
  @Invoker
  void callReadArguments() throws CommandSyntaxException;

  @Invoker
  void callBuildPredicate();
}
