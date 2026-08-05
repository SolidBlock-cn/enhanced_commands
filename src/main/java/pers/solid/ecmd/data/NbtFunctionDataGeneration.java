package pers.solid.ecmd.data;

import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.ResourceKey;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.nbt.function.CompoundNbtFunction;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.nbt.function.RegexReplaceNbtFunction;
import pers.solid.ecmd.nbt.function.SimpleNbtFunction;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class NbtFunctionDataGeneration implements DynamicRegistryGenerationBridge<NbtFunction> {
  private static ResourceKey<NbtFunction> of(String value) {
    return ResourceKey.create(NbtFunction.REGISTRY_KEY, EnhancedCommands.id(value));
  }

  @Override
  public String getBridgeName() {
    return "NBT Functions (Enhanced Commands)";
  }

  @Override
  public void configureBridge(ContextBridge<NbtFunction> context) {
    context.add(of("examples/convert_to_mangrove"), new RegexReplaceNbtFunction(Pattern.compile("(dark_oak|pale_oak|oak|spruce|jungle|acacia)"), "mangrove", true, true, Optional.empty()));
    context.add(of("examples/set_count_to_zero"), new CompoundNbtFunction(Map.of("Count", Optional.of(new SimpleNbtFunction(ShortTag.valueOf((short) 0)))), true));
  }
}
