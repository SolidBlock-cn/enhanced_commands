package pers.solid.ecmd.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.FlatLevelGeneratorPresetTags;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;
import pers.solid.ecmd.worldgen.EnhancedFlatKeys;

import java.util.concurrent.CompletableFuture;

public class FlatLevelGeneratorPresetTagProvider extends FabricTagProvider<FlatLevelGeneratorPreset> {
  public FlatLevelGeneratorPresetTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
    super(output, RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET, registriesFuture);
  }

  @Override
  protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.CRIMSON_FOREST);
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.DEEP_DARK);
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.FLOWER_FOREST);
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.FROZEN_OCEAN);
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.MUSHROOM_FIELD);
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.SAVANNA);
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.THE_NETHER);
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.WARM_OCEAN);
    getOrCreateTagBuilder(FlatLevelGeneratorPresetTags.VISIBLE).addOptional(EnhancedFlatKeys.WARPED_FOREST);
  }

}
