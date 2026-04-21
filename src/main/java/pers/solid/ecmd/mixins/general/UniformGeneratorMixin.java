package pers.solid.ecmd.mixins.general;

import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

@Mixin(UniformGenerator.class)
public abstract class UniformGeneratorMixin implements NumberProviderExtension {
  @Shadow
  @Final
  private NumberProvider min;

  @Shadow
  @Final
  private NumberProvider max;

  public int getInt(ExecutionContext executionContext) {
    return Mth.nextInt(executionContext.random, ((NumberProviderExtension) min).getInt(executionContext), ((NumberProviderExtension) max).getInt(executionContext));
  }

  public float getFloat(ExecutionContext executionContext) {
    return Mth.nextFloat(executionContext.random, ((NumberProviderExtension) min).getFloat(executionContext), ((NumberProviderExtension) max).getFloat(executionContext));
  }

  @Override
  public String asString$enhancedCommands() {
    if (min instanceof ConstantValue(float minValue) && max instanceof ConstantValue(float maxValue)) {
      return StringUtil.nf.format(minValue) + ".." + StringUtil.nf.format(maxValue);
    } else {
      return "uniform(" + ((NumberProviderExtension) min).asString$enhancedCommands() + ", " + ((NumberProviderExtension) max).asString$enhancedCommands() + ")";
    }
  }
}
