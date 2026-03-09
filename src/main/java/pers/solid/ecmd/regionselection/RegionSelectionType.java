package pers.solid.ecmd.regionselection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Contract;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.api.RegistryBridge;

import java.util.function.Supplier;

public interface RegionSelectionType {
  ResourceKey<Registry<RegionSelectionType>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("region_selection_type"));
  Registry<RegionSelectionType> REGISTRY = RegistryBridge.createRegistry(REGISTRY_KEY, true);
  Codec<RegionSelectionType> CODEC = REGISTRY.byNameCodec();
  StreamCodec<RegistryFriendlyByteBuf, RegionSelectionType> PACKET_CODEC = ByteBufCodecs.registry(REGISTRY_KEY);

  /**
   * 创建一个属于此类型的新的区域选择。
   *
   * @return 属于此类型的新的 {@link RegionSelection} 对象。
   */
  @Contract(value = "-> new", pure = true)
  RegionSelection createRegionSelection();

  MapCodec<? extends RegionSelection> codec();

  default RegionSelection createRegionSelectionFrom(RegionSelection source) {
    final RegionSelection regionSelection = createRegionSelection();
    regionSelection.inheritPointsFrom(source);
    return regionSelection;
  }

  record Impl<T extends RegionSelection>(Supplier<RegionSelection> newSupplier, MapCodec<T> codec) implements RegionSelectionType {
    @Override
    public RegionSelection createRegionSelection() {
      return newSupplier.get();
    }
  }
}
