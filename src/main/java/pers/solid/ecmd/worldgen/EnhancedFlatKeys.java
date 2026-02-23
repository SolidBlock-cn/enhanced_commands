package pers.solid.ecmd.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import pers.solid.ecmd.EnhancedCommands;

public final class EnhancedFlatKeys {
  private EnhancedFlatKeys() {
  }

  public static final ResourceKey<FlatLevelGeneratorPreset> DEEP_DARK = of("deep_dark");
  public static final ResourceKey<FlatLevelGeneratorPreset> FROZEN_OCEAN = of("frozen_ocean");
  public static final ResourceKey<FlatLevelGeneratorPreset> FLOWER_FOREST = of("flower_forest");
  public static final ResourceKey<FlatLevelGeneratorPreset> MUSHROOM_FIELD = of("mushroom_field");
  public static final ResourceKey<FlatLevelGeneratorPreset> SAVANNA = of("savanna");
  public static final ResourceKey<FlatLevelGeneratorPreset> THE_NETHER = of("the_nether");
  public static final ResourceKey<FlatLevelGeneratorPreset> WARM_OCEAN = of("warm_ocean");
  public static final ResourceKey<FlatLevelGeneratorPreset> WARPED_FOREST = of("warped_forest");
  public static final ResourceKey<FlatLevelGeneratorPreset> CRIMSON_FOREST = of("crimson_forest");

  private static ResourceKey<FlatLevelGeneratorPreset> of(String value) {
    return ResourceKey.create(Registries.FLAT_LEVEL_GENERATOR_PRESET, EnhancedCommands.id(value));
  }
}
