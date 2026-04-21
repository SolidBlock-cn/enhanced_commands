package pers.solid.ecmd.mixins.general;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

@Mixin(BinomialDistributionGenerator.class)
public abstract class BinomialDistributionGeneratorMixin implements NumberProviderExtension {
  @Shadow
  @Final
  private NumberProvider n;

  @Shadow
  @Final
  private NumberProvider p;

  public int getInt(ExecutionContext executionContext) {
    int i = ((NumberProviderExtension) n).getInt(executionContext);
    float f = ((NumberProviderExtension) p).getFloat(executionContext);
    RandomSource randomSource = executionContext.random;
    int j = 0;

    for (int k = 0; k < i; ++k) {
      if (randomSource.nextFloat() < f) {
        ++j;
      }
    }

    return j;
  }

  public float getFloat(ExecutionContext executionContext) {
    return (float) this.getInt(executionContext);
  }

  @Override
  public String asString$enhancedCommands() {
    return "binomial(" + ((NumberProviderExtension) n).asString$enhancedCommands() + ", " + ((NumberProviderExtension) p).asString$enhancedCommands() + ")";
  }
}
