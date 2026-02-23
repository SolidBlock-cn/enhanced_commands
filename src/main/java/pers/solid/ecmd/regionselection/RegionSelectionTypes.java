package pers.solid.ecmd.regionselection;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import org.apache.commons.lang3.Validate;
import pers.solid.ecmd.EnhancedCommands;

import java.util.function.Supplier;

public final class RegionSelectionTypes {
  public static final RegionSelectionType CUBOID = register(BlockCuboidRegionSelection::new, BlockCuboidRegionSelection.CODEC, "cuboid");
  public static final RegionSelectionType EXTENSION = register(ExtensionCuboidRegionSelection::new, ExtensionCuboidRegionSelection.CODEC, "extension");
  public static final RegionSelectionType SPHERE = register(SphereRegionSelection::new, SphereRegionSelection.CODEC, "sphere");

  private RegionSelectionTypes() {
  }

  private static <R extends RegionSelection> RegionSelectionType.Impl<R> register(Supplier<RegionSelection> newSupplier, MapCodec<R> codec, String name) {
    return Registry.register(RegionSelectionType.REGISTRY, EnhancedCommands.id(name), new RegionSelectionType.Impl<>(newSupplier, codec));
  }

  public static void init() {
    Validate.notEmpty(RegionSelectionType.REGISTRY.entrySet());
  }
}
