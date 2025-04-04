package pers.solid.ecmd.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.StructureSet;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorLayer;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.worldgen.EnhancedFlatKeys;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FlatLevelGeneratorPresetDataGeneration extends FabricDynamicRegistryProvider {
  public FlatLevelGeneratorPresetDataGeneration(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
    super(output, registriesFuture);
  }

  protected static RegistryKey<FlatLevelGeneratorPreset> of(String value) {
    return RegistryKey.of(RegistryKeys.FLAT_LEVEL_GENERATOR_PRESET, EnhancedCommands.id(value));
  }

  @Override
  protected void configure(RegistryWrapper.WrapperLookup wrapperLookup, Entries entries) {
    entries.add(EnhancedFlatKeys.CRIMSON_FOREST, preset(wrapperLookup,
        Items.CRIMSON_FENCE,
        BiomeKeys.CRIMSON_FOREST,
        null,
        true,
        true,
        new FlatChunkGeneratorLayer(1, Blocks.CRIMSON_NYLIUM),
        new FlatChunkGeneratorLayer(62, Blocks.NETHERRACK),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
    entries.add(EnhancedFlatKeys.DEEP_DARK, preset(wrapperLookup,
        Items.SCULK_SENSOR,
        BiomeKeys.DEEP_DARK,
        null,
        true,
        false,
        new FlatChunkGeneratorLayer(1, Blocks.SCULK),
        new FlatChunkGeneratorLayer(3, Blocks.DIRT),
        new FlatChunkGeneratorLayer(59, Blocks.STONE),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
    entries.add(EnhancedFlatKeys.FLOWER_FOREST, preset(wrapperLookup,
        Items.BLUE_ORCHID,
        BiomeKeys.FLOWER_FOREST,
        null,
        true,
        true,
        new FlatChunkGeneratorLayer(1, Blocks.GRASS_BLOCK),
        new FlatChunkGeneratorLayer(3, Blocks.DIRT),
        new FlatChunkGeneratorLayer(59, Blocks.STONE),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
    entries.add(EnhancedFlatKeys.FROZEN_OCEAN, preset(wrapperLookup,
        Items.ICE,
        BiomeKeys.FROZEN_OCEAN,
        null,
        true,
        false,
        new FlatChunkGeneratorLayer(1, Blocks.ICE),
        new FlatChunkGeneratorLayer(15, Blocks.WATER),
        new FlatChunkGeneratorLayer(5, Blocks.GRAVEL),
        new FlatChunkGeneratorLayer(5, Blocks.DIRT),
        new FlatChunkGeneratorLayer(22, Blocks.STONE),
        new FlatChunkGeneratorLayer(15, Blocks.DEEPSLATE),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
    entries.add(EnhancedFlatKeys.MUSHROOM_FIELD, preset(wrapperLookup,
        Items.BROWN_MUSHROOM,
        BiomeKeys.MUSHROOM_FIELDS,
        null,
        true,
        true,
        new FlatChunkGeneratorLayer(1, Blocks.MYCELIUM),
        new FlatChunkGeneratorLayer(3, Blocks.DIRT),
        new FlatChunkGeneratorLayer(59, Blocks.STONE),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
    entries.add(EnhancedFlatKeys.SAVANNA, preset(wrapperLookup,
        Items.ACACIA_SAPLING,
        BiomeKeys.SAVANNA,
        null,
        true,
        true,
        new FlatChunkGeneratorLayer(1, Blocks.GRASS_BLOCK),
        new FlatChunkGeneratorLayer(3, Blocks.DIRT),
        new FlatChunkGeneratorLayer(59, Blocks.STONE),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
    entries.add(EnhancedFlatKeys.THE_NETHER, preset(wrapperLookup,
        Items.NETHERRACK,
        BiomeKeys.NETHER_WASTES,
        null,
        true,
        true,
        new FlatChunkGeneratorLayer(63, Blocks.NETHERRACK),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
    entries.add(EnhancedFlatKeys.WARM_OCEAN, preset(wrapperLookup,
        Items.BUBBLE_CORAL,
        BiomeKeys.WARM_OCEAN,
        null,
        true,
        false,
        new FlatChunkGeneratorLayer(16, Blocks.WATER),
        new FlatChunkGeneratorLayer(5, Blocks.GRAVEL),
        new FlatChunkGeneratorLayer(5, Blocks.DIRT),
        new FlatChunkGeneratorLayer(22, Blocks.STONE),
        new FlatChunkGeneratorLayer(15, Blocks.DEEPSLATE),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
    entries.add(EnhancedFlatKeys.WARPED_FOREST, preset(wrapperLookup,
        Items.WARPED_FUNGUS,
        BiomeKeys.WARPED_FOREST,
        null,
        true,
        true,
        new FlatChunkGeneratorLayer(1, Blocks.WARPED_NYLIUM),
        new FlatChunkGeneratorLayer(62, Blocks.NETHERRACK),
        new FlatChunkGeneratorLayer(1, Blocks.BEDROCK)));
  }

  public static FlatLevelGeneratorPreset preset(RegistryWrapper.WrapperLookup wrapperLookup, ItemConvertible icon, RegistryKey<Biome> biome, @Nullable Set<RegistryKey<StructureSet>> structureSetKeys, boolean hasFeatures, boolean hasLakes, FlatChunkGeneratorLayer... layers) {
    RegistryEntryLookup<StructureSet> structureSetLookup = wrapperLookup.getWrapperOrThrow(RegistryKeys.STRUCTURE_SET);
    RegistryEntryLookup<PlacedFeature> placedFeatureLookup = wrapperLookup.getWrapperOrThrow(RegistryKeys.PLACED_FEATURE);
    RegistryEntryLookup<Biome> biomeLookup = wrapperLookup.getWrapperOrThrow(RegistryKeys.BIOME);
    @Nullable RegistryEntryList.Direct<StructureSet> structureSets = structureSetKeys == null ? null : RegistryEntryList.of(structureSetLookup::getOrThrow, structureSetKeys);
    FlatChunkGeneratorConfig flatChunkGeneratorConfig = new FlatChunkGeneratorConfig(Optional.ofNullable(structureSets), biomeLookup.getOrThrow(biome), FlatChunkGeneratorConfig.getLavaLakes(placedFeatureLookup));
    if (hasFeatures) {
      flatChunkGeneratorConfig.enableFeatures();
    }

    if (hasLakes) {
      flatChunkGeneratorConfig.enableLakes();
    }

    for (int i = layers.length - 1; i >= 0; --i) {
      flatChunkGeneratorConfig.getLayers().add(layers[i]);
    }

    return new FlatLevelGeneratorPreset(Registries.ITEM.getEntry(icon.asItem()), flatChunkGeneratorConfig);
  }

  @Override
  public String getName() {
    return "Flat Level Generator Preset (Enhanced Commands)";
  }
}
