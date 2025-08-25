package pers.solid.ecmd;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;
import pers.solid.ecmd.function.block.BlockFunction;

import java.util.Map;

public class BlockFunctionResourceReloadListener extends JsonDataLoader<BlockFunction> implements IdentifiableResourceReloadListener {
  private static final Identifier ID = EnhancedCommands.id("block_function");
  private static final Logger LOGGER = LogUtils.getLogger();
  private final RegistryWrapper.WrapperLookup registryLookup;
  private Map<Identifier, BlockFunction> blockFunctionsById = ImmutableMap.of();

  public BlockFunctionResourceReloadListener(RegistryWrapper.WrapperLookup registryLookup) {
    super(BlockFunction.CODEC, ResourceFinder.json(RegistryKeys.getPath(BlockFunction.REGISTRY_KEY)));
    this.registryLookup = registryLookup;
  }

  @Override
  public Identifier getFabricId() {
    return ID;
  }

  @Override
  protected void apply(Map<Identifier, BlockFunction> prepared, ResourceManager manager, Profiler profiler) {
    ImmutableMap.Builder<Identifier, BlockFunction> builder = ImmutableMap.builder();

    for (Map.Entry<Identifier, BlockFunction> entry : prepared.entrySet()) {
      Identifier identifier = entry.getKey();

      try {
        builder.put(identifier, entry.getValue());
      } catch (IllegalArgumentException | JsonParseException var12) {
        LOGGER.error("Parsing error loading block function {}", identifier, var12);
      }
    }

    this.blockFunctionsById = builder.build();
    LOGGER.info("Loaded {} block functions", this.blockFunctionsById.size());
  }
}
