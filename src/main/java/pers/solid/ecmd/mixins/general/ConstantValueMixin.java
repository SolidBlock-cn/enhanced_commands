package pers.solid.ecmd.mixins.general;

import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

@Mixin(ConstantValue.class)
public abstract class ConstantValueMixin implements NumberProviderExtension {
  @Shadow
  @Final
  private float value;

  @Override
  public float getFloat(ExecutionContext executionContext) {
    return value;
  }

  @Override
  public int getInt(ExecutionContext executionContext) {
    return Math.round(value);
  }

  @Override
  public String asString$enhancedCommands() {
    return StringUtil.nf.format(value);
  }
}
