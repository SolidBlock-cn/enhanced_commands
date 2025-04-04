package pers.solid.ecmd.worldgen;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;
import pers.solid.ecmd.EnhancedCommands;

public final class EnhancedFlatKeys {
  private EnhancedFlatKeys() {
  }

  public static final RegistryKey<FlatLevelGeneratorPreset> DEEP_DARK = of("deep_dark");
  public static final RegistryKey<FlatLevelGeneratorPreset> FROZEN_OCEAN = of("frozen_ocean");
  public static final RegistryKey<FlatLevelGeneratorPreset> FLOWER_FOREST = of("flower_forest");
  public static final RegistryKey<FlatLevelGeneratorPreset> MUSHROOM_FIELD = of("mushroom_field");
  public static final RegistryKey<FlatLevelGeneratorPreset> SAVANNA = of("savanna");
  public static final RegistryKey<FlatLevelGeneratorPreset> THE_NETHER = of("the_nether");
  public static final RegistryKey<FlatLevelGeneratorPreset> WARM_OCEAN = of("warm_ocean");
  public static final RegistryKey<FlatLevelGeneratorPreset> WARPED_FOREST = of("warped_forest");
  public static final RegistryKey<FlatLevelGeneratorPreset> CRIMSON_FOREST = of("crimson_forest");

  private static RegistryKey<FlatLevelGeneratorPreset> of(String value) {
    return RegistryKey.of(RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET, EnhancedCommands.id(value));
  }
}
