package pers.solid.ecmd;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;
import pers.solid.ecmd.function.block.BlockFunction;

import java.util.Map;

public class BlockFunctionResourceReloadListener extends JsonDataLoader implements IdentifiableResourceReloadListener {
  private static final Identifier ID = EnhancedCommands.id("block_function");
  private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
  private static final Logger LOGGER = LogUtils.getLogger();
  private final RegistryWrapper.WrapperLookup registryLookup;
  private Map<Identifier, BlockFunction> blockFunctionsById = ImmutableMap.of();

  public BlockFunctionResourceReloadListener(RegistryWrapper.WrapperLookup registryLookup) {
    super(GSON, RegistryKeys.getPath(BlockFunction.REGISTRY_KEY));
    this.registryLookup = registryLookup;
  }

  @Override
  public Identifier getFabricId() {
    return ID;
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
    ImmutableMap.Builder<Identifier, BlockFunction> builder = ImmutableMap.builder();
    RegistryOps<JsonElement> registryOps = this.registryLookup.getOps(JsonOps.INSTANCE);

    for (Map.Entry<Identifier, JsonElement> entry : prepared.entrySet()) {
      Identifier identifier = entry.getKey();

      try {
        BlockFunction blockFunction = BlockFunction.CODEC.parse(registryOps, entry.getValue()).getOrThrow(JsonParseException::new);
        builder.put(identifier, blockFunction);
      } catch (IllegalArgumentException | JsonParseException var12) {
        LOGGER.error("Parsing error loading block function {}", identifier, var12);
      }
    }

    this.blockFunctionsById = builder.build();
    LOGGER.info("Loaded {} block functions", this.blockFunctionsById.size());
  }
}
