package pers.solid.ecmd.item.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

public final class RandomItemFunction implements ItemFunction {
  public static final MapCodec<RandomItemFunction> CODEC = MapCodec.unit(RandomItemFunction::new);

  private transient @Nullable FeatureFlagSet featureSet;
  private transient Item @Nullable [] items;

  private Item[] getItems(RegistryAccess rm, FeatureFlagSet fs) {
    if (items == null || featureSet != fs) {
      return (items = calculateItems(rm, fs));
    }
    return items;
  }

  private Item[] calculateItems(RegistryAccess rm, FeatureFlagSet fs) {
    this.featureSet = fs;
    return rm.lookupOrThrow(Registries.ITEM).stream().filter(block -> block.isEnabled(fs)).toArray(Item[]::new);
  }

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    final Level world = context.positionProvider.getWorld$ec();
    final Item[] items = getItems(world.registryAccess(), world.enabledFeatures());
    final Item item = items[context.random.nextInt(items.length)];
    return new ItemStack(item);
  }

  @Override
  public ItemFunctionType<RandomItemFunction> getType() {
    return ItemFunctionTypes.RANDOM;
  }

  @Override
  public String asString() {
    return "*";
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof RandomItemFunction;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
