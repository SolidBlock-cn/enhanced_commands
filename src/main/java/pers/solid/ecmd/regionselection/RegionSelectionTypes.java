package pers.solid.ecmd.regionselection;

import com.mojang.serialization.MapCodec;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.InitializeContext;
import pers.solid.ecmd.api.RegistryBridge;

import java.util.function.Supplier;

public final class RegionSelectionTypes {
  private static final RegistryBridge<RegionSelectionType> REGISTRY_BRIDGE = RegistryBridge.create(EnhancedCommands.MOD_ID, RegionSelectionType.REGISTRY);

  public static final RegionSelectionType CUBOID = register(BlockCuboidRegionSelection::new, BlockCuboidRegionSelection.CODEC, "cuboid");
  public static final RegionSelectionType EXTENSION = register(ExtensionCuboidRegionSelection::new, ExtensionCuboidRegionSelection.CODEC, "extension");
  public static final RegionSelectionType SPHERE = register(SphereRegionSelection::new, SphereRegionSelection.CODEC, "sphere");

  private RegionSelectionTypes() {
  }

  private static <R extends RegionSelection> RegionSelectionType.Impl<R> register(Supplier<RegionSelection> newSupplier, MapCodec<R> codec, String name) {
    return REGISTRY_BRIDGE.register(name, new RegionSelectionType.Impl<>(newSupplier, codec));
  }

  public static void init(InitializeContext context) {
    context.registerRegistry(RegionSelectionType.REGISTRY);
    context.validateAndRegister(REGISTRY_BRIDGE);
  }
}
