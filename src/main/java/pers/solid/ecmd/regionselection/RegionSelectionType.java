package pers.solid.ecmd.regionselection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.Contract;
import pers.solid.ecmd.EnhancedCommands;

import java.util.function.Supplier;

public interface RegionSelectionType {
  RegistryKey<Registry<RegionSelectionType>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("region_builder_type"));
  Registry<RegionSelectionType> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();
  Codec<RegionSelectionType> CODEC = REGISTRY.getCodec();
  PacketCodec<RegistryByteBuf, RegionSelectionType> PACKET_CODEC = PacketCodecs.registryValue(REGISTRY_KEY);

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
