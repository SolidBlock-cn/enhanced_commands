package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.data.BlockFunctionDataGeneration;

import java.util.concurrent.CompletableFuture;

public class BlockFunctionDataGenerationImpl extends FabricDynamicRegistryProvider implements DynamicRegistryGenerationBridgeImpl<BlockFunction>, BlockFunctionDataGeneration {
  public BlockFunctionDataGenerationImpl(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void configure(HolderLookup.Provider provider, Entries entries) {
    configureBridge(provider, entries);
  }

  @Override
  public String getName() {
    return "Block Functions (Enhanced Commands)";
  }

  public static TagKey<Block> oresConventionalTag() {
    return ConventionalBlockTags.ORES;
  }
}
