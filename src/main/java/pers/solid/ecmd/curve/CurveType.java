package pers.solid.ecmd.curve;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.region.RegionType;
import pers.solid.ecmd.util.parse.ParseContext;

public interface CurveType<T extends Curve> extends RegionType<T> {
  RegistryKey<Registry<CurveType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(EnhancedCommands.id("curve_type"));
  Registry<CurveType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

  @Nullable
  default CurveArgument<?> parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return null;
  }
}
