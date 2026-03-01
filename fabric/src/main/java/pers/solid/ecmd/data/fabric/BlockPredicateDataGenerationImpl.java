package pers.solid.ecmd.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.data.BlockPredicateDataGeneration;
import pers.solid.ecmd.predicate.block.BlockPredicate;

import java.util.concurrent.CompletableFuture;

public class BlockPredicateDataGenerationImpl extends FabricDynamicRegistryProvider implements BlockPredicateDataGeneration, DynamicRegistryGenerationBridgeImpl<BlockPredicate> {
  public BlockPredicateDataGenerationImpl(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
    super(output, registriesFuture);
  }

  @Override
  protected void configure(HolderLookup.Provider provider, Entries entries) {
    configureBridge(provider, entries);
  }

  @Override
  public @NotNull String getName() {
    return "Block Predicates (Enhanced Commands)";
  }

  public static TagKey<Block> conventionalBudsTag() {
    return ConventionalBlockTags.BUDS;
  }
}
